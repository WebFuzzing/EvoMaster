package org.evomaster.core.problem.rest.service


import com.google.inject.Inject
import org.evomaster.core.StaticCounter
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * take care of calculating/collecting fitness of [RestIndividual]
 */
class RestResourceFitness : AbstractRestFitness<RestIndividual>() {



    @Inject
    private lateinit var sampler : ResourceSampler

    @Inject
    private lateinit var dm: ResourceDepManageService

    @Inject
    private lateinit var rm: ResourceManageService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestResourceFitness::class.java)
    }

    /*
        add db check in term of each abstract resource
     */
    override fun doCalculateCoverage(individual: RestIndividual, targets: Set<Int>): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        /*
            there might some dbaction between rest actions.
            This map is used to record the key mapping in SQL, e.g., PK, FK
         */
        val sqlIdMap = mutableMapOf<Long, Long>()
        val executedDbActions = mutableListOf<DbAction>()

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //whether there exist some SQL execution failure
        var failureBefore = doDbCalls(individual.seeInitializingActions(), sqlIdMap, false, executedDbActions, actionResults)

        val cookies = getCookies(individual)
        val tokens = getTokens(individual)

        val fv = FitnessValue(individual.size().toDouble())

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        var indexOfAction = 0

        for (call in individual.getResourceCalls()) {

            val result = doDbCalls(call.seeActions(ActionFilter.ONLY_SQL) as List<DbAction>, sqlIdMap, failureBefore, executedDbActions, actionResults)
            failureBefore = failureBefore || result

            var terminated = false

            for (a in call.seeActions(ActionFilter.NO_SQL)){

                //TODO handling of inputVariables
                registerNewAction(a, indexOfAction)

                var ok = false

                if (a is RestCallAction) {
                    ok = handleRestCall(a, actionResults, chainState, cookies, tokens)
                    // update creation of resources regarding response status
                    val restActionResult = actionResults.filterIsInstance<RestCallResult>()[indexOfAction]
                    call.getResourceNode().confirmFailureCreationByPost(call, a, restActionResult)

                } else {
                    throw IllegalStateException("Cannot handle: ${a.javaClass}")
                }

                if (!ok) {
                    terminated = true
                    break
                }
                indexOfAction++
            }

            if(terminated)
                break
        }

        val allRestResults = actionResults.filterIsInstance<RestCallResult>()
        val dto = restActionResultHandling(individual, targets, allRestResults, fv)?:return null

        /*
            TODO: Man shall we update the action cluster based on expanded action?
         */
        individual.seeActions().forEach {
            val node = rm.getResourceNodeFromCluster(it.path.toString())
            node.updateActionsWithAdditionalParams(it)
        }

        /*
         update dependency regarding executed dto
         */
        if(config.extractSqlExecutionInfo && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.updateResourceTables(individual, dto)

        return EvaluatedIndividual(
                fv, individual.copy() as RestIndividual, actionResults, config = config, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals)

    }

    /**
     * @param allDbActions specified the db actions to be executed
     * @param sqlIdMap indicates the map id of pk to generated id
     * @param allSuccessBefore indicates whether all SQL before this [allDbActions] are executed successfully
     * @param previous specified the previous db actions which have been executed
     * @return whether [allDbActions] execute successfully
     */
    private fun doDbCalls(allDbActions : List<DbAction>,
                          sqlIdMap : MutableMap<Long, Long>,
                          allSuccessBefore : Boolean,
                          previous: MutableList<DbAction>,
                          actionResults: MutableList<ActionResult>
    ) : Boolean {

        if (allDbActions.isEmpty()) {
            return true
        }


        val dbresults = (allDbActions.indices).map { DbActionResult() }

        if (allDbActions.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            // other dbactions might bind with the representExistingData, so we still need to record sqlId here.
            allDbActions.filter { it.representExistingData }.flatMap { it.seeGenes() }.filterIsInstance<SqlPrimaryKeyGene>().forEach {
                sqlIdMap.putIfAbsent(it.uniqueId, it.uniqueId)
            }
            previous.addAll(allDbActions)
            actionResults.addAll(dbresults)
            return true
        }

        val dto = try {
            DbActionTransformer.transform(allDbActions, sqlIdMap, previous)
        }catch (e : IllegalArgumentException){
            // the failure might be due to previous failure
            if (!allSuccessBefore){
                previous.addAll(allDbActions)
                return false
            } else
                throw e
        }
        dto.idCounter = StaticCounter.getAndIncrease()

        val sqlResults = rc.executeDatabaseInsertionsAndGetIdMapping(dto)
        val map = sqlResults?.idMapping
        val executedResults = sqlResults?.executionResults

        if (executedResults?.size?:0 > allDbActions.size)
            throw IllegalStateException("incorrect insertion execution results (${executedResults!!.size}) which is more than the size of insertions (${allDbActions.size}).")
        executedResults?.forEachIndexed { index, b ->
//            allDbActions[index].insertExecutedSuccessfully = b
            dbresults[index].setInsertExecutionResult(b)
        }
        actionResults.addAll(dbresults)
        previous.addAll(allDbActions)


        if (map == null) {
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
            return false
        }else{
            val expected = allDbActions.filter { !it.representExistingData }
                .flatMap { it.seeGenes() }.flatMap { it.flatView() }
                .filterIsInstance<SqlPrimaryKeyGene>()
                .filter { it.gene is SqlAutoIncrementGene }
                .filterNot { it.gene is SqlForeignKeyGene }
            val missing = expected.filterNot {
                map.containsKey(it.uniqueId)
            }
            sqlIdMap.putAll(map)
            if (missing.isNotEmpty()){
                log.warn("can not get sql ids for {} from sut", missing.map { "${it.uniqueId} of ${it.tableName}" }.toSet().joinToString(","))
                return false
            }
        }
        return true
    }

    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}