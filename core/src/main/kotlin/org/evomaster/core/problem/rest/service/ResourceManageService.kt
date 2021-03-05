package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.schema.Table
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.*
import org.evomaster.core.problem.rest.resource.dependency.CreationChain
import org.evomaster.core.problem.rest.resource.dependency.PostCreationChain
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
        private const val PROB_EXTRA_PATCH = 0.8
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
     * init resource nodes based on [actionCluster] and [sqlInsertBuilder]
     */
    fun initResourceNodes(actionCluster : MutableMap<String, Action>, sqlInsertBuilder: SqlInsertBuilder? = null) {
        if (resourceCluster.isNotEmpty()) return
        if (this.sqlInsertBuilder!= null) return

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

        resourceCluster.values.forEach{it.init(config.isEnabledResourceWithSQL() || notEmptyDb())}

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
                val call = sampleOneAction(ar, a.copy() as RestAction)
                call.actions.forEach {a->
                    if(a is RestCallAction) a.auth = auth
                }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}.forEach { a->
                val call = sampleOneAction(ar, a.copy() as RestAction)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a is RestCallAction && a.verb == HttpVerb.POST } != null && it.getPostChain()?.actions?.run { this.size > 1 }?:false  }
                .forEach { ar->
                    genPostChain(ar, config.maxTestSize)?.let {call->
                        call.actions.forEach { (it as RestCallAction).auth = auth }
                        adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.PUT }.forEach {a->
                val call = sampleOneAction(ar, a.copy() as RestAction)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //template
        sortedResources.forEach { ar->
            ar.getTemplates().values.filter { t-> RestResourceTemplateHandler.isNotSingleAction(t.template) }
                    .forEach {ct->
                        val call = sampleRestResourceCalls(ar, ct.template, config.maxTestSize)
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
        val candidate = randomness.choose(getResourceCluster().filterNot { r-> existingRs.contains(r.key) }.keys)
        return sampleAnyRestResourceCalls(resourceCluster[candidate]!!,maxTestSize)
    }

    /************************** sampling *********************************/

    fun generateAnother(node: RestResourceNode, calls : RestResourceCalls, maxTestSize: Int) : RestResourceCalls?{
        val current = calls.template?.template?:RestResourceTemplateHandler.getStringTemplateByActions(calls.actions.filterIsInstance<RestCallAction>())
        val rest = node.getTemplates().filter { it.value.template != current}
        if(rest.isEmpty()) return null
        val selected = randomness.choose(rest.keys)
        return genCalls(node, selected, maxTestSize)

    }

    fun randomRestResourceCalls(node: RestResourceNode, maxTestSize: Int): RestResourceCalls{
        val randomTemplates = node.getTemplates().filter { e->
            e.value.size in 1..maxTestSize
        }.map { it.key }
        if(randomTemplates.isEmpty()) return sampleOneAction(node)
        return genCalls(node, randomness.choose(randomTemplates),  maxTestSize)
    }

    fun sampleIndResourceCall(node: RestResourceNode, maxTestSize: Int) : RestResourceCalls{
        node.selectTemplate({ call : CallsTemplate -> call.independent || (call.template == HttpVerb.POST.toString() && call.size > 1)}, randomness)?.let {
            return genCalls(node, it.template, maxTestSize, false, false)
        }
        return genCalls(node, HttpVerb.POST.toString(), maxTestSize)
    }


    fun sampleOneAction(node: RestResourceNode, verb : HttpVerb? = null) : RestResourceCalls{
        val al = node.sampleOneAvailableAction(verb, randomness)
        return sampleOneAction(node, al)
    }

    fun sampleOneAction(node: RestResourceNode, action : RestAction) : RestResourceCalls{
        val copy = action.copy()
        node.randomizeActionGenes(copy as RestCallAction, randomness)

        val template = node.getTemplates()[copy.verb.toString()]
            ?: throw IllegalArgumentException("${copy.verb} is not one of templates of ${node.path}")
        val call =  RestResourceCalls(template, RestResourceInstance(node, copy.parameters), mutableListOf(copy))

        if(action is RestCallAction && action.verb == HttpVerb.POST){
            node.getCreation { c : CreationChain -> (c is PostCreationChain) }.let {
                if(it != null && (it as PostCreationChain).actions.size == 1 && it.isComplete()){
                    call.status = ResourceStatus.CREATED_REST
                }else{
                    call.status = ResourceStatus.NOT_FOUND_DEPENDENT
                }
            }
        }else
            call.status = ResourceStatus.NOT_EXISTING

        return call
    }

    /**
     * sample a resource call without a specific template
     * @param key a key of resource
     * @param maxTestSize a maximum size of rest actions in this resource calls
     * @param prioriIndependent specify whether to prioritize independent templates
     * @param prioriDependent specify whether to prioritize dependent templates
     */
    fun sampleAnyRestResourceCalls(key: String, maxTestSize: Int, prioriIndependent : Boolean = false, prioriDependent : Boolean = false): RestResourceCalls{
        val node = getResourceNodeFromCluster(key)
        return sampleAnyRestResourceCalls(node, maxTestSize, prioriIndependent, prioriDependent)
    }

    private fun sampleAnyRestResourceCalls(
        node: RestResourceNode, maxTestSize: Int, prioriIndependent : Boolean = false, prioriDependent : Boolean = false): RestResourceCalls {
        if (maxTestSize < 1 && prioriDependent == prioriIndependent && prioriDependent){
            throw IllegalArgumentException("unaccepted args")
        }
        val fchosen = node.getTemplates().filter { it.value.size <= maxTestSize }
        if(fchosen.isEmpty())
            return sampleOneAction(node)
        val chosen =
            if (prioriDependent)  fchosen.filter { !it.value.independent }
            else if (prioriIndependent) fchosen.filter { it.value.independent }
            else fchosen
        if (chosen.isEmpty())
            return genCalls(node, randomness.choose(fchosen).template, maxTestSize)
        return genCalls(node, randomness.choose(chosen).template,maxTestSize)
    }


    private fun sampleRestResourceCalls(node: RestResourceNode, template: String, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        return genCalls(node, template, maxTestSize)
    }

    private fun genPostChain(node: RestResourceNode, maxTestSize: Int) : RestResourceCalls?{
        val template = node.getTemplates()["POST"]?:
        return null

        return genCalls(node, template.template, maxTestSize)
    }

    /**
     * generate ResourceCall for [node]
     * @param node the resource
     * @param template specify a template for the resource calls
     * @param maxTestSize is a maximum number of rest actions in this resource calls
     * @param checkSize specify whether to check the size constraint
     * @param createResource specify whether to preprate the resource for the calls
     * @param additionalPatch specify whether to add additional patch
     *
     * TODO update postCreation accordingly
     */
    fun genCalls(
        node: RestResourceNode,
        template : String,
        maxTestSize : Int = 1,
        checkSize : Boolean = true,
        createResource : Boolean = true,
        additionalPatch : Boolean = true
    ) : RestResourceCalls{
        if(!node.getTemplates().containsKey(template))
            throw IllegalArgumentException("$template does not exist in ${node.path}")

        val ats = RestResourceTemplateHandler.parseTemplate(template)
        val callsTemplate = node.getTemplates().getValue(template)

        val creationNeeded = createResource && ats[0] == HttpVerb.POST

        if (!creationNeeded)
            return generateRestActionsForCalls(node, ats, callsTemplate, maxTestSize, checkSize, createResource, additionalPatch)

        if (config.probOfApplySQLActionToCreateResources == 0.0 && !node.hasPostCreation())
            throw IllegalStateException("for resource ${node.path}, there does not exist any POST/PUT to create actions and the SQL creation is alo disabled")

        val withSql = !node.hasPostCreation() || randomness.nextBoolean(config.probOfApplySQLActionToCreateResources)

        if (!withSql)
            return generateRestActionsForCalls(node, ats, callsTemplate, maxTestSize, checkSize, createResource, additionalPatch)

        TODO()
    }

    private fun generateRestActionsForCalls(
        node: RestResourceNode,
        ats : Array<HttpVerb>,
        callsTemplate: CallsTemplate,
        maxTestSize : Int = 1,
        checkSize : Boolean = true,
        createResource : Boolean = true,
        additionalPatch : Boolean = true,
        checkPostChain : Boolean = true
    ) : RestResourceCalls{

        //val ats = RestResourceTemplateHandler.parseTemplate(template)

        val result : MutableList<RestAction> = mutableListOf()
        var resource : RestResourceInstance? = null

        val skipBind : MutableList<RestAction> = mutableListOf()

        var isCreated = ResourceStatus.NOT_EXISTING

        if(createResource && ats[0] == HttpVerb.POST){
            val nonPostIndex = ats.indexOfFirst { it != HttpVerb.POST }
            val ac = node.getActionByHttpVerb( if(nonPostIndex==-1) HttpVerb.POST else ats[nonPostIndex])!!.copy() as RestCallAction
            node.randomizeActionGenes(ac, randomness)
            result.add(ac)

            isCreated = node.createResourcesFor(ac, result, maxTestSize , randomness, checkSize && (!callsTemplate.sizeAssured))

            if(checkPostChain && !callsTemplate.sizeAssured){
                node.getPostChain()?:throw IllegalStateException("fail to init post creation")
                val pair = node.checkDifferenceOrInit(postactions = (if(ac.verb == HttpVerb.POST) result else result.subList(0, result.size - 1)).map { (it as RestCallAction).copy() as RestCallAction}.toMutableList())
                if (!pair.first) {
                    RestResourceNode.log.warn("the post action are not matched with initialized post creation.")
                }
                else {
                    node.updateTemplateSize()
                }
            }

            val lastPost = result.last()
            resource = RestResourceInstance(node, (lastPost as RestCallAction).parameters)
            skipBind.addAll(result)
            if(nonPostIndex == -1){
                (1 until ats.size).forEach{ _ ->
                    result.add(lastPost.copy().also {
                        skipBind.add(it as RestAction)
                    } as RestAction)
                }
            }else{
                if(nonPostIndex != ats.size -1){
                    (nonPostIndex + 1 until ats.size).forEach {
                        val action = node.getActionByHttpVerb(ats[it])!!.copy() as RestCallAction
                        node.randomizeActionGenes(action, randomness)
                        result.add(action)
                    }
                }
            }

        }else{
            ats.forEach {at->
                val ac = (node.getActionByHttpVerb(at)?:throw IllegalArgumentException("cannot find $at verb in ${
                    node.actions.joinToString(
                        ","
                    ) { a -> a.getName() }
                }")).copy() as RestCallAction
                node.randomizeActionGenes(ac, randomness)
                result.add(ac)
            }

            if(resource == null)
                resource = node.createResourceInstance(result, randomness, skipBind)
            if(checkPostChain){
                callsTemplate.sizeAssured = (result.size  == callsTemplate.size)
            }
        }

        if(result.size > 1)
            result.filterNot { ac -> skipBind.contains(ac) }.forEach { ac ->
                if((ac as RestCallAction).parameters.isNotEmpty()){
                    ac.bindToSamePathResolution(ac.path, resource.params)
                }
            }

        assert(result.isNotEmpty())

        if(additionalPatch
            && randomness.nextBoolean(PROB_EXTRA_PATCH)
            &&!callsTemplate.independent
            && callsTemplate.template.contains(HttpVerb.PATCH.toString()) && result.size + 1 <= maxTestSize){

            val index = result.indexOfFirst { (it is RestCallAction) && it.verb == HttpVerb.PATCH }
            val copy = result.get(index).copy() as RestAction
            result.add(index, copy)
        }
        val calls = RestResourceCalls(callsTemplate, resource, result)

        calls.status = isCreated

        return calls
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

        if(!doesCreateResource){
            val call = sampleIndResourceCall(ar,size)
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

        val call = genCalls(ar, candidate, size,true, true)
        calls.add(call)

        if(hasDBHandler()){
            if(call.status != ResourceStatus.CREATED_REST
                    || dm.checkIfDeriveTable(call)
                    || candidateForInsertion != null
            ){

                /*
                    derive possible db, and bind value according to db
                */
                val created = handleDbActionForCall( call, forceInsert, false)
                if(!created){
                    //shall we only print the error msg only once?
                    LoggingUtil.uniqueWarn(log, "resource creation for $resourceKey fails")
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
            if(forceInsert){
                generateInsertSql(tableName, dbActions)
            }else if(forceSelect){
                if(getDataInDb(tableName) != null && getDataInDb(tableName)!!.isNotEmpty()) generateSelectSql(tableName, dbActions)
                else failToGenDB = true
            }else{
                if(getDataInDb(tableName)!= null ){
                    val size = getDataInDb(tableName)!!.size
                    when{
                        size < config.minRowOfTable -> generateInsertSql(tableName, dbActions).apply {
                            failToGenDB = failToGenDB || !this
                        }
                        else ->{
                            if(randomness.nextBoolean(config.probOfSelectFromDatabase)){
                                generateSelectSql(tableName, dbActions)
                            }else{
                                generateInsertSql(tableName, dbActions).apply {
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


    private fun handleDbActionForCall(call: RestResourceCalls, forceInsert: Boolean, forceSelect: Boolean) : Boolean{

        val paramToTables = dm.extractRelatedTablesForCall(call)
        if(paramToTables.isEmpty()) return false

        //val relatedTables = removeDuplicatedTables(paramToTables.values.flatMap { it.map { g->g.tableName } }.toSet())
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
            dm.bindCallWithDBAction(call,dbActions, paramToTables, dbRemovedDueToRepair = removed)

            call.dbActions.addAll(dbActions)
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

    private fun generateInsertSql(tableName : String, dbActions: MutableList<DbAction>) : Boolean{
        val insertDbAction =
                sqlInsertBuilder!!
                        .createSqlInsertionAction(tableName, forceAll = true)

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

    fun notEmptyDb() = getSqlBuilder() != null && getSqlBuilder()!!.anyTable()

    private fun getDataInDb(tableName: String) : MutableList<DataRowDto>?{
        val found = dataInDB.filterKeys { k-> k.equals(tableName, ignoreCase = true) }.keys
        if (found.isEmpty()) return null
        assert(found.size == 1)
        return dataInDB.getValue(found.first())
    }

    fun getTableByName(name : String) = tables.keys.find { it.equals(name, ignoreCase = true) }?.run { tables[this] }


    /**
     * @return sorted [tables] based on their relationships
     * for instance, Table A refer to Table B, then in the returned list, A should be before B.
     */
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

    fun removeDuplicatedTables(tables: Set<String>) : List<String>{
        val sorted = sortTableBasedOnFK(tables)
        sorted.toMutableList().removeIf { t->
            sorted.any { s-> s!= t && s.foreignKeys.any { fk-> fk.targetTable.equals(t.name, ignoreCase = true) } }
        }
        return sorted.map { it.name }
    }
}