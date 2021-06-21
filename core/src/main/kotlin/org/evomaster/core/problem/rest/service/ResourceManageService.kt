package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.SqlInitResourceStrategy
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.schema.Table
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.*
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.AdaptiveParameterControl
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

    @Inject
    private lateinit var apc: AdaptiveParameterControl


    /**
     * a cluster for resource nodes in the sut
     */
    val cluster: ResourceCluster = ResourceCluster()


    private var sqlInsertBuilder : SqlInsertBuilder? = null

    /**
     * init resource nodes based on [actionCluster] and [sqlInsertBuilder]
     */
    fun initResourceNodes(actionCluster : MutableMap<String, Action>, sqlInsertBuilder: SqlInsertBuilder? = null) {

        if (this.sqlInsertBuilder!= null) return
        this.sqlInsertBuilder = sqlInsertBuilder

        cluster.initResourceCluster(actionCluster, sqlInsertBuilder, config)

        if(config.extractSqlExecutionInfo && config.doesApplyNameMatching){
            cluster.initRelatedTables()

            if(config.probOfEnablingResourceDependencyHeuristics > 0.0)
                dm.initDependencyBasedOnDerivedTables(resourceCluster = cluster)
        }
        if(config.doesApplyNameMatching && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.deriveDependencyBasedOnSchema(cluster)
    }


    /**
     * this function is used to initialized ad-hoc individuals for resource-based individual
     */
    fun createAdHocIndividuals(auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividual>){
        val sortedResources = cluster.getCluster().values.sortedByDescending { it.getTokenMap().size }.asSequence()

        //GET, PATCH, DELETE
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                call.seeActions(ActionFilter.NO_SQL).forEach { a->
                    if(a is RestCallAction) a.auth = auth
                }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                (call.seeActions(ActionFilter.NO_SQL) as List<RestCallAction>).forEach { it.auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a is RestCallAction && a.verb == HttpVerb.POST } != null && it.getPostChain()?.actions?.run { this.size > 1 }?:false  }
                .forEach { ar->
                    ar.genPostChain(randomness, config.maxTestSize)?.let {call->
                        call.seeActions(ActionFilter.NO_SQL).forEach { (it as RestCallAction).auth = auth }
                        adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                call.seeActions(ActionFilter.NO_SQL).forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //template
        sortedResources.forEach { ar->
            ar.getTemplates().values.filter { t-> RestResourceTemplateHandler.isNotSingleAction(t.template) }
                    .forEach {ct->
                        val call = ar.sampleRestResourceCalls(ct.template, randomness, config.maxTestSize)
                        call.seeActions(ActionFilter.NO_SQL).forEach { if(it is RestCallAction) it.auth = auth }
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
        val candidate = randomness.choose(getResourceCluster().filterNot { r-> existingRs.contains(r.key) }.keys)
        return cluster.getResourceNode(candidate)!!.sampleAnyRestResourceCalls(randomness,maxTestSize )
    }


    /**
     * sample an resource call which refers to [resourceKey]
     * @param resourceKey a key refers to an resource node
     * @param doesCreateResource whether to prepare an resource for the call
     * @param calls existing calls
     * @param forceSQLInsert force to use insertion to prepare the resource, otherwise prior to use POST
     * @param bindWith the sampled resource call requires to bind values according to [bindWith]
     * @param template is to specify a template to be employed for sampling a call which is nullable
     */
    fun sampleCall(resourceKey: String,
                   doesCreateResource: Boolean,
                   calls : MutableList<RestResourceCalls>,
                   size : Int,
                   forceSQLInsert: Boolean = false,
                   bindWith : MutableList<RestResourceCalls>? = null,
                   template: String? = null
    ){
        val ar = cluster.getResourceNode(resourceKey)
                ?: throw IllegalArgumentException("resource path $resourceKey does not exist!")

        if(!doesCreateResource ){
            val call = ar.sampleIndResourceCall(randomness,size)
            calls.add(call)
            //TODO shall we control the probability to sample GET with an existing resource.
            if(hasDBHandler() && call.template?.template == HttpVerb.GET.toString() && randomness.nextBoolean(0.5)){
                val created = handleDbActionForCall(call, false, true, false)
            }
            return
        }

        var employSQL = hasDBHandler() && ar.getDerivedTables().isNotEmpty()
                && (forceSQLInsert || randomness.nextBoolean(config.probOfApplySQLActionToCreateResources))

        var candidate = template

        if (candidate == null){
            var candidateForInsertion : String? = null
            if(employSQL){
                //Insert - GET/PUT/PATCH
                val candidates = ar.getTemplates().filter { it.value.isSingleAction() }
                candidateForInsertion = if(candidates.isNotEmpty()) randomness.choose(candidates.keys) else null
                employSQL = candidateForInsertion != null
            }

            candidate = if(candidateForInsertion.isNullOrBlank()) {
                //prior to select the template with POST
                ar.getTemplates().filter { !it.value.independent }.run {
                    if(isNotEmpty())
                        randomness.choose(this.keys)
                    else
                        randomness.choose(ar.getTemplates().keys)
                }
            } else candidateForInsertion
        }


        val call = ar.createRestResourceCallBasedOnTemplate(candidate, randomness, size)
        calls.add(call)

        if(hasDBHandler() && config.probOfApplySQLActionToCreateResources > 0){
            if(call.status != ResourceStatus.CREATED_REST
                    || dm.checkIfDeriveTable(call)
                    || employSQL
            ){
                /*
                    derive possible db, and bind value according to db
                */
                call.is2POST = candidate == "POST" && employSQL //&& (randomness.nextBoolean(0.1) || forceSQLInsert)

                val created = handleDbActionForCall(call, forceSQLInsert, false, call.is2POST)
                if(!created){
                    LoggingUtil.uniqueWarn(log, "resource creation for $resourceKey fails")
                }else{
                    call.status =  ResourceStatus.CREATED_SQL
                }
            }
        }

        if(bindWith != null){
            dm.bindCallWithFront(call, bindWith)
        }
    }


    private fun generateDbActionForCall(forceInsert: Boolean, forceSelect: Boolean, dbActions: MutableList<DbAction>, relatedTables : List<String>) : Boolean{
        var failToGenDB = false

        snapshotDB()

        relatedTables.forEach { tableName->
            val dataInDb = cluster.getDataInDb(tableName)
            if(forceInsert){
                generateInsertSql(tableName, dbActions, true)
            }else if(forceSelect){
                if(dataInDb?.isNotEmpty() == true) generateSelectSql(tableName, dbActions)
                else failToGenDB = true
            }else{
                if(dataInDb!= null ){
                    val size = dataInDb.size
                    when{
                        size < config.minRowOfTable -> generateInsertSql(tableName, dbActions, true).apply {
                            failToGenDB = failToGenDB || !this
                        }
                        else ->{
                            if(randomness.nextBoolean(config.probOfSelectFromDatabase)){
                                generateSelectSql(tableName, dbActions)
                            }else{
                                generateInsertSql(tableName, dbActions, true).apply {
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

    /**
     * regarding resource call handling, if there exist two resource calls in an individual
     * e.g., A and B. besides, A and B are related to two tables, i.e., TA and TB respectively.
     * during search, if there exist a possible dependency, e.g., B-> A, and we prepare resource with SQL.
     * For resource B, with SQL, to handle fk, we typically generate two SQL, one is for TB and other is for TA.
     * In this case, since the TA has been generated by A, we do not need to generate extra TA in TB handing.
     * So we shrink such SQL actions with this method.
     */
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
                    DbActionUtils.repairFK(dbAction, previous, created, getSqlBuilder(), randomness)
                previous.add(dbAction)
            }

            dbActions.addAll(0, created)
        }
    }


    private fun handleDbActionForCall(
        call: RestResourceCalls,
        forceInsert: Boolean,
        forceSelect: Boolean,
        employSQL: Boolean
    ) : Boolean{

        val paramToTables = dm.extractRelatedTablesForCall(call, withSql = employSQL)
        if(paramToTables.isEmpty()) return false

        val relatedTables = paramToTables.values.flatMap { it.map { g->g.tableName } }.toSet()

        val dbActions = mutableListOf<DbAction>()
        val failToGenDb = generateDbActionForCall( forceInsert = forceInsert, forceSelect = forceSelect, dbActions = dbActions, relatedTables = relatedTables.toList())

        if(failToGenDb) return false

        containTables(dbActions, relatedTables)

        if(dbActions.isNotEmpty()){

            DbActionUtils.randomizeDbActionGenes(dbActions, randomness)
            val removed = repairDbActionsForResource(dbActions)

            //shrinkDbActions(dbActions)

            /*
             TODO bind data according to action or dbaction?

             Note that since we prepare data for rest actions, we bind values of dbaction based on rest actions.

             */
            //call.buildBindingWithDbActions(dbActions, bindingMap = paramToTables, cluster = cluster, forceBindParamBasedOnDB = false, dbRemovedDueToRepair = removed)
            //call.addDbAction(actions = dbActions)
            call.initDbActions(dbActions, cluster, false, removed)

        }
        return paramToTables.isNotEmpty() && !failToGenDb
    }


    private fun containTables(dbActions: MutableList<DbAction>, tables: Set<String>) : Boolean{

        val missing = tables.filter { t-> dbActions.none { d-> d.table.name.equals(t, ignoreCase = true) } }
        if (missing.isNotEmpty())
            log.warn("missing rows of tables {} to be created.", missing.joinToString(",") { it })
        return missing.isEmpty()
    }


    /**
     *  repair dbaction of resource call after standard mutation
     *  Since standard mutation does not change structure of a test, the involved tables
     *  should be same with previous.
     */
//    fun repairRestResourceCalls(call: RestResourceCalls) {
//        call.repairGenesAfterMutation(resourceCluster)
//
//        if(hasDBHandler() && call.dbActions.isNotEmpty()){
//
//            val previous = call.dbActions.map { it.table.name }
//            call.dbActions.clear()
//            //handleCallWithDBAction(referResource, call, true, false)
//            handleDbActionForCall(call, forceInsert = true, forceSelect = false)
//
//            if(call.dbActions.size != previous.size){
//                //remove additions
//                call.dbActions.removeIf {
//                    !previous.contains(it.table.name)
//                }
//            }
//        }
//    }
    /*********************************** database ***********************************/

    private fun selectToDataRowDto(dbAction : DbAction, tableName : String) : DataRowDto{
        dbAction.seeGenes().forEach { assert((it is SqlPrimaryKeyGene || it is ImmutableDataHolderGene || it is SqlForeignKeyGene)) }
        val set = dbAction.seeGenes().filter { it is SqlPrimaryKeyGene }.map { ((it as SqlPrimaryKeyGene).gene as ImmutableDataHolderGene).value }.toSet()
        return randomness.choose(cluster.getDataInDb(tableName)!!.filter { it.columnData.toSet().equals(set) })
    }

    private fun hasDBHandler() : Boolean = sqlInsertBuilder!=null

    private fun snapshotDB(){
        if(hasDBHandler()){
            cluster.syncDataInDb(sqlInsertBuilder)
        }
    }

    fun repairDbActionsForResource(dbActions: MutableList<DbAction>) : Boolean{
        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(dbActions.flatMap { it.seeGenes() })

        return DbActionUtils.repairBrokenDbActionsList(dbActions, randomness)
        //DbActionUtils.repairFkForInsertions(dbActions)
    }


    private fun generateSelectSql(tableName : String, dbActions: MutableList<DbAction>, forceDifferent: Boolean = false, withDbAction: DbAction?=null){
        if(dbActions.map { it.table.name }.contains(tableName)) return

        val info = cluster.getDataInDb(tableName)

        Lazy.assert {
            info?.isNotEmpty() == true && (!forceDifferent || withDbAction == null)
        }
        val columns = if(forceDifferent && withDbAction!!.representExistingData){
            selectToDataRowDto(withDbAction, tableName)
        }else {
            randomness.choose(info!!)
        }

        val selectDbAction = sqlInsertBuilder!!.extractExistingByCols(tableName, columns)
        dbActions.add(selectDbAction)
    }

    @Deprecated("will be removed")
    private fun generateInsertSql(
        tableName: String,
        dbActions: MutableList<DbAction>,
        doNotCreateDuplicatedTable: Boolean = true
    ) : Boolean{

        val insertDbAction =
                sqlInsertBuilder!!
                        .createSqlInsertionAction(tableName, forceAll = true)

        if (log.isTraceEnabled){
            log.trace("at generateInserSql, {} insertions are added, and they are {}", insertDbAction.size,
                insertDbAction.joinToString(",") {
                    it.getResolvedName()
                })
        }

        if(insertDbAction.isEmpty()) return false

        if (!doNotCreateDuplicatedTable){
            dbActions.addAll(insertDbAction)
            return true
        }

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

    fun getResourceCluster()  = cluster.getCluster()

    fun getResourceNodeFromCluster(key : String) : RestResourceNode = cluster.getResourceNode(key)?: throw IllegalArgumentException("cannot find the resource with a key $key")

    fun getTableInfo() = cluster.getTableInfo()

    /**
     * @return SqlBuilder
     */
    fun getSqlBuilder() : SqlInsertBuilder?{
        if(!hasDBHandler()) return null
        return sqlInsertBuilder
    }

    /**
     * @return table class based on the specified [name]
     */
    private fun getTableByName(name : String) = cluster.getTableByName(name)


    /**
     * @return sorted [tables] based on their relationships
     * for instance, Table A refer to Table B, then in the returned list, A should be before B.
     */
    @Deprecated("will be removed")
    fun sortTableBasedOnFK(tables : Set<String>) : List<Table>{
        return tables.mapNotNull { getTableByName(it) }.sortedWith(
            Comparator { o1, o2 ->
                when {
                    o1.foreignKeys.any { t-> t.targetTable.equals(o2.name,ignoreCase = true) } -> 1
                    o2.foreignKeys.any { t-> t.targetTable.equals(o1.name,ignoreCase = true) } -> -1
                    else -> 0
                }
            }
        )
    }

    @Deprecated("will be removed")
    fun removeDuplicatedTables(tables: Set<String>) : List<String>{
        val sorted = sortTableBasedOnFK(tables)
        sorted.toMutableList().removeIf { t->
            sorted.any { s-> s!= t && s.foreignKeys.any { fk-> fk.targetTable.equals(t.name, ignoreCase = true) } }
        }

        return sorted.map { it.name }
    }

    fun getResourceNum() : Int {
        if (config.maxSqlInitActionsPerResource == 0) return 0
        return when(config.employSqlNumResourceStrategy){
            SqlInitResourceStrategy.NONE -> 0
            SqlInitResourceStrategy.RANDOM -> config.maxSqlInitActionsPerResource
            SqlInitResourceStrategy.DPC -> apc.getExploratoryValue(config.maxSqlInitActionsPerResource, 1)
        }
    }
}