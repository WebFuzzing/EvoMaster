package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.SqlInitResourceStrategy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.resource.*
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.gene.GeneUtils
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
            ar.actions.filter { it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                call.seeActions(ActionFilter.NO_SQL).forEach { ra->
                    if(ra is RestCallAction) ra.auth = auth
                }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it.verb == HttpVerb.POST}.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                (call.seeActions(ActionFilter.NO_SQL) as List<RestCallAction>).forEach { it.auth = auth }
                adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a.verb == HttpVerb.POST } != null && it.getPostChain()?.actions?.run { this.size > 1 }?:false  }
                .forEach { ar->
                    ar.genPostChain(randomness, config.maxTestSize)?.let {call->
                        call.seeActions(ActionFilter.NO_SQL).forEach { (it as RestCallAction).auth = auth }
                        adHocInitialIndividuals.add(RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it.verb == HttpVerb.PUT }.forEach {a->
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
            if(hasDBHandler() && config.probOfApplySQLActionToCreateResources > 0 && call.template?.template == HttpVerb.GET.toString() && randomness.nextBoolean(0.5)){
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
                call.is2POST = candidate == "POST" && employSQL
                        && (forceSQLInsert
                            || randomness.nextBoolean(0.1)  // here we provide a low chance to employ SQL, since the rest creation might be false
                            || (call.status != ResourceStatus.CREATED_REST && randomness.nextBoolean(0.5)) // here we provide a further chance to employ SQL
                        )

                val created = handleDbActionForCall(
                    call, forceSQLInsert, false, call.is2POST,
                    previousDbActions = bindWith?.flatMap { it.seeActions(ActionFilter.ONLY_SQL) as List<DbAction>} ?: listOf())

                if(!created){
                    LoggingUtil.uniqueWarn(log, "resource creation for $resourceKey fails")
                }else{
                    call.status =  ResourceStatus.CREATED_SQL
                }
            }
        }

        if(bindWith != null){
            call.bindWithOtherRestResourceCalls(bindWith, cluster,true)
        }
    }

    private fun handleDbActionForCall(
        call: RestResourceCalls,
        forceInsert: Boolean = false,
        forceSelect: Boolean = false,
        employSQL: Boolean,
        previousDbActions: List<DbAction> = listOf()
    ) : Boolean{

        val paramToTables = dm.extractRelatedTablesForCall(call, withSql = employSQL)
        if(paramToTables.isEmpty()) return false

        val relatedTables = paramToTables.values.flatMap { it.map { g->g.tableName } }

        val employSQLSelect = (!forceInsert) && (forceSelect || employSelect(relatedTables))

        val dbActions = cluster.createSqlAction(
            relatedTables, getSqlBuilder()!!, previousDbActions,
            doNotCreateDuplicatedAction = true, isInsertion = !employSQLSelect,
            randomness = randomness)

        if(dbActions.isNotEmpty()){

            val removed = repairDbActionsForResource(dbActions)
            call.initDbActions(dbActions, cluster, false, removed, bindWith = null)

        }
        return paramToTables.isNotEmpty()
    }


    private fun employSelect(tables : List<String>) : Boolean{
        return randomness.nextBoolean(config.probOfSelectFromDatabase) && tables.any {
            cluster.getDataInDb(it)?.size?:0 >= config.minRowOfTable
        }
    }

    // might be useful for debugging
    private fun containTables(dbActions: MutableList<DbAction>, tables: Set<String>) : Boolean{

        val missing = tables.filter { t-> dbActions.none { d-> d.table.name.equals(t, ignoreCase = true) } }
        if (missing.isNotEmpty())
            log.warn("missing rows of tables {} to be created.", missing.joinToString(",") { it })
        return missing.isEmpty()
    }

    /*********************************** database ***********************************/


    private fun hasDBHandler() : Boolean = sqlInsertBuilder!=null

    private fun repairDbActionsForResource(dbActions: MutableList<DbAction>) : Boolean{
        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(dbActions.flatMap { it.seeGenes() })

        return DbActionUtils.repairBrokenDbActionsList(dbActions, randomness)
    }


    /*********************************** utility ***********************************/

    /**
     * @return all resource node
     */
    fun getResourceCluster()  = cluster.getCluster()

    /**
     * @return a resource node based on the specified [key]
     */
    fun getResourceNodeFromCluster(key : String) : RestResourceNode = cluster.getResourceNode(key)?: throw IllegalArgumentException("cannot find the resource with a key $key")

    /**
     * @return table info
     */
    fun getTableInfo() = cluster.getTableInfo()

    /**
     * @return SqlBuilder
     */
    fun getSqlBuilder() : SqlInsertBuilder?{
        if(!hasDBHandler()) return null
        return sqlInsertBuilder
    }


    /**
     * @return a maximum number of resources to be manipulated in the initialization with SQL
     *          e.g., we can add N resource or delete N resource in the initialization per time with e.g., structure mutator
     */
    fun getSqlMaxNumOfResource() : Int {
        if (config.maxSqlInitActionsPerResource == 0) return 0
        return when(config.employSqlNumResourceStrategy){
            SqlInitResourceStrategy.NONE -> 0
            SqlInitResourceStrategy.RANDOM -> config.maxSqlInitActionsPerResource
            SqlInitResourceStrategy.DPC -> apc.getExploratoryValue(config.maxSqlInitActionsPerResource, 1)
        }
    }
}