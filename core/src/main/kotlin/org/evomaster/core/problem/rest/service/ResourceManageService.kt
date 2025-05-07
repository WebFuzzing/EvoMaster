package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.SqlInitResourceStrategy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.resource.*
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.utils.GeneUtils
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

    /**
     * a cluster for resource nodes which are not part of action cluster
     */
    private val excludedCluster : MutableMap<String, ExcludedResourceNode> = mutableMapOf()


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

    fun initExcludedResourceNode(actions: List<RestCallAction>){
        actions.groupBy { it.path.toString() }.forEach {g->
            val path = RestPath(g.key)
            excludedCluster.putIfAbsent(g.key, ExcludedResourceNode(path, g.value.toMutableList()))
        }
    }

    /**
     * this function is used to initialized ad-hoc individuals for resource-based individual
     */
    fun createAdHocIndividuals(auth: HttpWsAuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividual>, maxTestSize: Int){
        val sortedResources = cluster.getCluster().values.sortedByDescending { it.getTokenMap().size }.asSequence()

        //GET, PATCH, DELETE
        sortedResources.forEach { ar->
            ar.actions.filter { it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                call.seeActions(ActionFilter.NO_SQL).forEach { ra->
                    if(ra is RestCallAction) ra.auth = auth
                }
                adHocInitialIndividuals.add(
                    RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE)//.apply { this.computeTransitiveBindingGenes() }
                )
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it.verb == HttpVerb.POST}.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                (call.seeActions(ActionFilter.NO_SQL) as List<RestCallAction>).forEach { it.auth = auth }
                adHocInitialIndividuals.add(
                    RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE)//.apply { this.computeTransitiveBindingGenes() }
                )
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a.verb == HttpVerb.POST } != null && it.getPostChain()?.actions?.run { this.size > 1 }?:false  }
                .forEach { ar->
                    ar.genPostChain(randomness, maxTestSize)?.let {call->
                        call.seeActions(ActionFilter.NO_SQL).forEach { (it as RestCallAction).auth = auth }
                        adHocInitialIndividuals.add(
                            RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE)//.apply { this.computeTransitiveBindingGenes() }
                        )
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it.verb == HttpVerb.PUT }.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestCallAction, randomness)
                call.seeActions(ActionFilter.NO_SQL).forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(
                    RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE)//.apply { this.computeTransitiveBindingGenes() }
                )
            }
        }

        //template
        sortedResources.forEach { ar->
            ar.getTemplates().values.filter { t-> RestResourceTemplateHandler.isNotSingleAction(t.template) }
                    .forEach {ct->
                        val call = ar.sampleRestResourceCalls(ct.template, randomness, maxTestSize)
                        call.seeActions(ActionFilter.NO_SQL).forEach { if(it is RestCallAction) it.auth = auth }
                        adHocInitialIndividuals.add(
                            RestIndividual(mutableListOf(call), SampleType.SMART_RESOURCE)//.apply { this.computeTransitiveBindingGenes() }
                        )
                    }
        }

    }

    /**
     * handle to generate an resource call for addition in [ind]
     * @return the generated resource call
     */
    fun handleAddResource(ind : RestIndividual, maxTestSize : Int) : RestResourceCalls {
        val existingRs = ind.getResourceCalls().map { it.getResourceNodeKey() }
        val nonExistingRs = getResourceCluster().filterNot { r-> existingRs.contains(r.key) }.keys
        val candidate = if (nonExistingRs.isNotEmpty()) randomness.choose(nonExistingRs) else randomness.choose(existingRs)
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
            if(config.shouldGenerateSqlData() && hasDBHandler() && config.probOfApplySQLActionToCreateResources > 0 && call.template?.template == HttpVerb.GET.toString() && randomness.nextBoolean(0.5)){
                val created = handleDbActionForCall(call, false, true, false)
            }
            return
        }

        var employSQL = config.shouldGenerateSqlData() && hasDBHandler() && ar.getDerivedTables().isNotEmpty()
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

        if(config.shouldGenerateSqlData() && hasDBHandler() && config.probOfApplySQLActionToCreateResources > 0){
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
                    previousSqlActions = bindWith?.flatMap { it.seeActions(ActionFilter.ONLY_SQL) as List<SqlAction>} ?: listOf())

                if(!created){
                    /*
                        To test an action with a given state of SUT, we could manipulate its resource with SQL and post/put endpoints.
                        however, corresponding resources of the action might not be able to be prepared due to lack of info
                            e.g., with schema, do not find its post/put
                                with name matching of action URL and table names, do not identify SQL tables related to the action
                                based on execution, do not find accessed tables when executing the action
                     */
                    log.debug("it is unlikely to prepare a resource for $resourceKey with SQL/REST action")
                    //LoggingUtil.uniqueWarn(log, "it is unlikely to prepare a resource for $resourceKey with SQL/REST action")
                }else{
                    call.status =  ResourceStatus.CREATED_SQL
                }
            }
        }

        if(bindWith != null){
            call.bindWithOtherRestResourceCalls(bindWith, cluster,true, randomness = randomness)
        }
    }

    private fun handleDbActionForCall(
        call: RestResourceCalls,
        forceInsert: Boolean = false,
        forceSelect: Boolean = false,
        employSQL: Boolean,
        previousSqlActions: List<SqlAction> = listOf()
    ) : Boolean{

        val paramToTables = dm.extractRelatedTablesForCall(call, withSql = employSQL)
        if(paramToTables.isEmpty()) return false

        val relatedTables = paramToTables.values.flatMap { it.map { g->g.tableName } }

        val employSQLSelect = (!forceInsert) && (forceSelect || employSelect(relatedTables))

        val extraConstraints = randomness.nextBoolean(apc.getExtraSqlDbConstraintsProbability())
        val enableSingleInsertionForTable = randomness.nextBoolean(config.probOfEnablingSingleInsertionForTable)

        val dbActions = cluster.createSqlAction(
            relatedTables, getSqlBuilder()!!, previousSqlActions,
            doNotCreateDuplicatedAction = true, isInsertion = !employSQLSelect,
            randomness = randomness,
            useExtraSqlDbConstraints = extraConstraints,
            enableSingleInsertionForTable = enableSingleInsertionForTable)

        if(dbActions.isNotEmpty()){
            //FIXME cannot repair before it is mounted
            var removed = false; //repairDbActionsForResource(dbActions)
            call.initDbActions(dbActions, cluster, false, removed, randomness, bindWith = null)
            removed = !repairDbActionsForResource(dbActions) // FIXME
            if(removed){
                call.resetDbAction(dbActions)
                /*
                    FIXME this breaks things with binding...
                    however, as we are going to refactor DB actions, we can ignored for now.
                    TODO once refactored, need to put back the disabled test:
                    ResourceBasedTestInterface.testWithDatabaseAndNameAnalysis
                 */
            }
        }
        return paramToTables.isNotEmpty()
    }


    private fun employSelect(tables : List<String>) : Boolean{
        return randomness.nextBoolean(config.probOfSelectFromDatabase) && tables.any {
            cluster.getDataInDb(it)?.size?:0 >= config.minRowOfTable
        }
    }

    // might be useful for debugging
    private fun containTables(sqlActions: MutableList<SqlAction>, tables: Set<String>) : Boolean{

        val missing = tables.filter { t-> sqlActions.none { d-> d.table.name.equals(t, ignoreCase = true) } }
        if (missing.isNotEmpty())
            log.warn("missing rows of tables {} to be created.", missing.joinToString(",") { it })
        return missing.isEmpty()
    }

    /*********************************** database ***********************************/


    private fun hasDBHandler() : Boolean = sqlInsertBuilder!=null

    private fun repairDbActionsForResource(sqlActions: MutableList<SqlAction>) : Boolean{
        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(sqlActions.flatMap { it.seeTopGenes() })

        return SqlActionUtils.repairBrokenDbActionsList(sqlActions, randomness)
    }


    /*********************************** utility ***********************************/

    /**
     * @return all resource node
     */
    fun getResourceCluster()  = cluster.getCluster()

    /**
     * @return a resource node based on the specified [key]
     */
    fun getResourceNodeFromCluster(key : String) : RestResourceNode =
        cluster.getResourceNode(key)?: excludedCluster[key]?: throw IllegalArgumentException("cannot find the resource with a key $key")

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
     * @return a maximum number of resources to be manipulated with action or sql
     *          e.g., we can add N resource or delete N resource in the initialization per time with e.g., structure mutator
     */
    fun getMaxNumOfResourceSizeHandling() : Int {
        if (config.maxSizeOfHandlingResource == 0) return 0
        return when(config.employResourceSizeHandlingStrategy){
            SqlInitResourceStrategy.NONE -> 0
            SqlInitResourceStrategy.RANDOM -> config.maxSizeOfHandlingResource
            SqlInitResourceStrategy.DPC -> apc.getExploratoryValue(config.maxSizeOfHandlingResource, 1)
        }
    }
}
