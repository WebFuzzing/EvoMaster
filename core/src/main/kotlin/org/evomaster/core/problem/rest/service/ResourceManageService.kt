package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.InitMode
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.util.RestResourceTemplateHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * the class is used to manage all resources
 */
class ResourceManageService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ResourceManageService::class.java)
    }

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var dm : ResourceDepManageService

    /**
     * key is resource path
     * value is an abstract resource
     */
    private val resourceCluster : MutableMap<String, RestResourceNode> = mutableMapOf()

    /**
     * key is table name
     * value is a list of existing data of PKs in DB
     */
    private val dataInDB : MutableMap<String, MutableList<DataRowDto>> = mutableMapOf()

    /**
     * key is table name
     * value is the table
     */
    private val tables : MutableMap<String, Table> = mutableMapOf()

    private var sqlInsertBuilder : SqlInsertBuilder? = null
    /**
     * init resource nodes based on [actionCluster]
     */
    fun initResourceNodes(actionCluster : MutableMap<String, Action>, sqlInsertBuilder: SqlInsertBuilder? = null) {

        this.sqlInsertBuilder = sqlInsertBuilder

        if(config.extractSqlExecutionInfo) sqlInsertBuilder?.extractExistingTables(tables)

        actionCluster.values.forEach { u ->
            if (u is RestCallAction) {
                val resource = resourceCluster.getOrPut(u.path.toString()) {
                    RestResourceNode(
                            u.path.copy(),
                            initMode =
                                if(config.probOfEnablingResourceDependencyHeuristics > 0.0 && config.doesApplyNameMatching) InitMode.WITH_DERIVED_DEPENDENCY
                                else if(config.doesApplyNameMatching) InitMode.WITH_TOKEN
                                else if (config.probOfEnablingResourceDependencyHeuristics > 0.0) InitMode.WITH_DEPENDENCY
                                else InitMode.NONE)
                }
                resource.actions.add(u)
            }
        }
        resourceCluster.values.forEach{it.initAncestors(getResourceCluster().values.toList())}

        resourceCluster.values.forEach{it.init()}

        if(config.extractSqlExecutionInfo && config.doesApplyNameMatching){
            dm.initRelatedTables(resourceCluster.values.toMutableList(), getTableInfo())

            if(config.probOfEnablingResourceDependencyHeuristics > 0.0)
                dm.initDependencyBasedOnDerivedTables(resourceCluster.values.toList(), getTableInfo())
        }
        if(config.doesApplyNameMatching && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.deriveDependencyBasedOnSchema(resourceCluster.values.toList())
    }


    /**
     * this function is used to initialized ad-hoc individuals for resource-based individual
     */
    fun createAdHocIndividuals(auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividual>){
        val sortedResources = resourceCluster.values.sortedByDescending { it.getTokenMap().size }.asSequence()

        //GET, PATCH, DELETE
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach {a->
                    if(a is RestCallAction) a.auth = auth
                }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a is RestCallAction && a.verb == HttpVerb.POST } != null && it.getPostChain()?.actions?.run { this.size > 1 }?:false  }
                .forEach { ar->
                    ar.genPostChain(randomness, config.maxTestSize)?.let {call->
                        call.actions.forEach { (it as RestCallAction).auth = auth }
                        adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //template
        sortedResources.forEach { ar->
            ar.getTemplates().values.filter { t-> RestResourceTemplateHandler.isNotSingleAction(t.template) }
                    .forEach {ct->
                        val call = ar.sampleRestResourceCalls(ct.template, randomness, config.maxTestSize)
                        call.actions.forEach { if(it is RestCallAction) it.auth = auth }
                        adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
        }

    }

    /**
     * handle to generate an resource call for addition in [ind]
     * @return the generated resource call
     */
    fun handleAddResource(ind : RestIndividual, maxTestSize : Int) : RestResourceCalls {
        val existingRs = ind.getResourceCalls().map { it.getResourceNodeKey() }
        var candidate = randomness.choose(getResourceCluster().filterNot { r-> existingRs.contains(r.key) }.keys)
        return resourceCluster[candidate]!!.sampleAnyRestResourceCalls(randomness,maxTestSize )
    }


    /**
     * sample an resource call which refers to [resourceKey]
     * @param resourceKey a key refers to an resource node
     * @param doesCreateResource whether to prepare an resource for the call
     * @param calls existing calls
     * @param forceInsert force to use insertion to prepare the resource, otherwise prior to use POST
     * @param bindWith the sampled resource call requires to bind values according to [bindWith]
     */
    fun sampleCall(resourceKey: String, doesCreateResource: Boolean, calls : MutableList<RestResourceCalls>, size : Int, forceInsert: Boolean = false, bindWith : MutableList<RestResourceCalls>? = null){
        val ar = resourceCluster[resourceKey]
                ?: throw IllegalArgumentException("resource path $resourceKey does not exist!")

        if(!doesCreateResource ){
            val call = ar.sampleIndResourceCall(randomness,size)
            calls.add(call)
            //TODO shall we control the probability to sample GET with an existing resource.
            if(hasDBHandler() && call.template?.template == HttpVerb.GET.toString() && randomness.nextBoolean(0.5)){
                val created = handleDbActionForCall( call, false, true)
            }
            return
        }

        var candidateForInsertion : String? = null

        if(hasDBHandler() && ar.getDerivedTables().isNotEmpty() && (if(forceInsert) forceInsert else randomness.nextBoolean(config.probOfApplySQLActionToCreateResources))){
            //Insert - GET/PUT/PATCH
            val candidates = ar.getTemplates().filter { setOf("GET", "PUT", "PATCH").contains(it.value.template) && it.value.independent}
            candidateForInsertion = if(candidates.isNotEmpty()) randomness.choose(candidates.keys) else null
        }

        val candidate = if(candidateForInsertion.isNullOrBlank()) {
            //prior to select the template with POST
            ar.getTemplates().filter { !it.value.independent }.run {
                if(isNotEmpty())
                    randomness.choose(this.keys)
                else
                    randomness.choose(ar.getTemplates().keys)
            }
        } else candidateForInsertion

        val call = ar.genCalls(candidate, randomness, size,true, true)
        calls.add(call)

        if(hasDBHandler()){
            if(call.status != ResourceStatus.CREATED
                    || dm.checkIfDeriveTable(call)
                    || candidateForInsertion != null
            ){

                /*
                    derive possible db, and bind value according to db
                */
                val created = handleDbActionForCall( call, forceInsert, false)
                if(!created){
                    //TODO MAN record the call when postCreation fails
                }
            }
        }

        if(bindWith != null){
            dm.bindCallWithFront(call, bindWith)
        }
    }


    private fun generateDbActionForCall(forceInsert: Boolean, forceSelect: Boolean, dbActions: MutableList<DbAction>, relatedTables : Set<String>) : Boolean{
        var failToGenDB = false

        snapshotDB()

        relatedTables.forEach { tableName->
            if(forceInsert){
                generateInserSql(tableName, dbActions)
            }else if(forceSelect){
                if(getDataInDb(tableName) != null && getDataInDb(tableName)!!.isNotEmpty()) generateSelectSql(tableName, dbActions)
                else failToGenDB = true
            }else{
                if(getDataInDb(tableName)!= null ){
                    val size = getDataInDb(tableName)!!.size
                    when{
                        size < config.minRowOfTable -> generateInserSql(tableName, dbActions).apply {
                            failToGenDB = failToGenDB || !this
                        }
                        else ->{
                            if(randomness.nextBoolean(config.probOfSelectFromDatabase)){
                                generateSelectSql(tableName, dbActions)
                            }else{
                                generateInserSql(tableName, dbActions).apply {
                                    failToGenDB = failToGenDB || !this
                                }
                            }
                        }
                    }
                }else
                    failToGenDB = true
            }
        }
        return failToGenDB
    }

    private fun shrinkDbActions(dbActions: MutableList<DbAction>){
        val removedDbAction = mutableListOf<DbAction>()

        dbActions.forEachIndexed { index, dbAction ->
            if((0 until index).any { i -> dbActions[i].table.name == dbAction.table.name &&!dbActions[i].representExistingData })
                removedDbAction.add(dbAction)
        }

        if(removedDbAction.isNotEmpty()){
            dbActions.removeAll(removedDbAction)

            val previous = mutableListOf<DbAction>()
            val created = mutableListOf<DbAction>()

            dbActions.forEachIndexed { index, dbAction ->
                if(index != 0 && dbAction.table.foreignKeys.isNotEmpty() && dbAction.table.foreignKeys.find { fk -> removedDbAction.find { it.table.name == fk.targetTable } !=null } != null)
                    DbActionUtils.repairFK(dbAction, previous, created, getSqlBuilder())
                previous.add(dbAction)
            }

            dbActions.addAll(0, created)
        }
    }


    private fun handleDbActionForCall(call: RestResourceCalls, forceInsert: Boolean, forceSelect: Boolean) : Boolean{

        val paramToTables = dm.extractRelatedTablesForCall(call)
        if(paramToTables.isEmpty()) return false

        val relatedTables = paramToTables.values.flatMap { it.map { g->g.tableName } }.toSet()

        val dbActions = mutableListOf<DbAction>()

        val failToGenDb = generateDbActionForCall( forceInsert = forceInsert, forceSelect = forceSelect, dbActions = dbActions, relatedTables = relatedTables)

        if(failToGenDb) return false

        if(dbActions.isNotEmpty()){

            (0 until (dbActions.size - 1)).forEach { i ->
                (i+1 until dbActions.size).forEach { j ->
                    dbActions[i].table.foreignKeys.any { f->f.targetTable == dbActions[j].table.name}.let {
                        if(it){
                            val idb = dbActions[i]
                            dbActions[i] = dbActions[j]
                            dbActions[j] = idb
                        }
                    }
                }
            }
            DbActionUtils.randomizeDbActionGenes(dbActions, randomness)
            repairDbActions(dbActions)

            shrinkDbActions(dbActions)

            /*
             TODO bind data according to action or dbaction?

             Note that since we prepare data for rest actions, we bind values of dbaction based on rest actions.

             */
            dm.bindCallWithDBAction(call,dbActions, paramToTables)

            call.dbActions.addAll(dbActions)
        }
        return paramToTables.isNotEmpty() && !failToGenDb
    }



    /**
     *  repair dbaction of resource call after standard mutation
     *  Since standard mutation does not change structure of a test, the involved tables
     *  should be same with previous.
     */
    fun repairRestResourceCalls(call: RestResourceCalls) {
        call.repairGenesAfterMutation()

        if(hasDBHandler() && call.dbActions.isNotEmpty()){

            val previous = call.dbActions.map { it.table.name }
            call.dbActions.clear()
            //handleCallWithDBAction(referResource, call, true, false)
            handleDbActionForCall(call, forceInsert = true, forceSelect = false)

            if(call.dbActions.size != previous.size){
                //remove additions
                call.dbActions.removeIf {
                    !previous.contains(it.table.name)
                }
            }
        }
    }
    /*********************************** database ***********************************/

    private fun selectToDataRowDto(dbAction : DbAction, tableName : String) : DataRowDto{
        dbAction.seeGenes().forEach { assert((it is SqlPrimaryKeyGene || it is ImmutableDataHolderGene || it is SqlForeignKeyGene)) }
        val set = dbAction.seeGenes().filter { it is SqlPrimaryKeyGene }.map { ((it as SqlPrimaryKeyGene).gene as ImmutableDataHolderGene).value }.toSet()
        return randomness.choose(getDataInDb(tableName)!!.filter { it.columnData.toSet().equals(set) })
    }

    private fun hasDBHandler() : Boolean = sqlInsertBuilder!=null && (config.probOfApplySQLActionToCreateResources > 0.0)

    private fun snapshotDB(){
        if(hasDBHandler()){
            sqlInsertBuilder!!.extractExistingPKs(dataInDB)
        }
    }

    fun repairDbActions(dbActions: MutableList<DbAction>){
        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(dbActions.flatMap { it.seeGenes() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */

        DbActionUtils.repairBrokenDbActionsList(dbActions, randomness)
    }


    private fun generateSelectSql(tableName : String, dbActions: MutableList<DbAction>, forceDifferent: Boolean = false, withDbAction: DbAction?=null){
        if(dbActions.map { it.table.name }.contains(tableName)) return

        assert(getDataInDb(tableName) != null && getDataInDb(tableName)!!.isNotEmpty())
        assert(!forceDifferent || withDbAction == null)

        val columns = if(forceDifferent && withDbAction!!.representExistingData){
            selectToDataRowDto(withDbAction, tableName)
        }else {
            randomness.choose(getDataInDb(tableName)!!)
        }

        val selectDbAction = sqlInsertBuilder!!.extractExistingByCols(tableName, columns)
        dbActions.add(selectDbAction)
    }

    private fun generateInserSql(tableName : String, dbActions: MutableList<DbAction>) : Boolean{
        val insertDbAction =
                sqlInsertBuilder!!
                        .createSqlInsertionActionWithAllColumn(tableName)

        if(insertDbAction.isEmpty()) return false

        val pasted = mutableListOf<DbAction>()
        insertDbAction.reversed().forEach {ndb->
            val index = dbActions.indexOfFirst { it.table.name == ndb.table.name && !it.representExistingData}
            if(index == -1) pasted.add(0, ndb)
            else{
                if(pasted.isNotEmpty()){
                    dbActions.addAll(index+1, pasted)
                    pasted.clear()
                }
            }
        }

        if(pasted.isNotEmpty()){
            if(pasted.size == insertDbAction.size)
                dbActions.addAll(pasted)
            else
                dbActions.addAll(0, pasted)
        }
        return true
    }

    /*********************************** utility ***********************************/

    fun getResourceCluster()  = resourceCluster.toMap()

    fun getResourceNodeFromCluster(key : String) : RestResourceNode = resourceCluster[key]?: throw IllegalArgumentException("cannot find the resource with a key $key")

    fun getTableInfo() = tables.toMap()

    fun getSqlBuilder() : SqlInsertBuilder?{
        if(!hasDBHandler()) return null
        return sqlInsertBuilder
    }

    private fun getDataInDb(tableName: String) : MutableList<DataRowDto>?{
        val found = dataInDB.filterKeys { k-> k.equals(tableName, ignoreCase = true) }.keys
        if (found.isEmpty()) return null
        assert(found.size == 1)
        return dataInDB.getValue(found.first())
    }
}