package org.evomaster.core.problem.rest.service


import com.google.inject.Inject
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.Gene
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
        val previousDbGenes = mutableListOf<Gene>()

        //whether there exist some SQL execution failure
        var failureBefore = doDbCalls(individual.dbInitialization, sqlIdMap, false, previousDbGenes)

        val cookies = getCookies(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        var indexOfAction = 0

        for (call in individual.getResourceCalls()) {

            val result = doDbCalls(call.dbActions, sqlIdMap, failureBefore, previousDbGenes)

            failureBefore = failureBefore || result

            var terminated = false

            for (a in call.actions){

                //TODO handling of inputVariables
                registerNewAction(a, indexOfAction)

                var ok = false

                if (a is RestCallAction) {
                    ok = handleRestCall(a, actionResults, chainState, cookies)
                    /*
                    update creation of resources regarding response status
                     */
                    if (a.verb.run { this == HttpVerb.POST || this == HttpVerb.PUT} && call.status == ResourceStatus.CREATED_REST && (actionResults[indexOfAction] as RestCallResult).getStatusCode().run { this != 201 || this != 200 }){
                        call.getResourceNode().confirmFailureCreationByPost(call)
                    }

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

        val dto = restActionResultHandling(individual, targets, actionResults, fv)?:return null
        /*
         update dependency regarding executed dto
         */
        if(config.extractSqlExecutionInfo && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.updateResourceTables(individual, dto)

        /*
            TODO Man: shall we update SQL Insertion fails here for resource creation?
            then we prioritize to employ existing data if there exist
            but there might be various reasons which lead to the failure.
         */

        return EvaluatedIndividual(
                fv, individual.copy() as RestIndividual, actionResults, config = config, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals)

    }

    /**
     * @param allSuccessBefore indicates whether all SQL before this [allDbActions] are executed successfully
     * @return whether [allDbActions] execute successfully
     */
    private fun doDbCalls(allDbActions : List<DbAction>, sqlIdMap : MutableMap<Long, Long>, allSuccessBefore : Boolean, previous: MutableList<Gene>) : Boolean {

        if (allDbActions.isEmpty()) {
            return true
        }

        if (allDbActions.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return true
        }

        val dto = try {
            DbActionTransformer.transform(allDbActions, sqlIdMap, previous)
        }catch (e : IllegalArgumentException){
            // the failure might be due to previous failure
            if (!allSuccessBefore)
                return false
            else
                throw e
        }

        val map = rc.executeDatabaseInsertionsAndGetIdMapping(dto)
        if (map == null) {
            LoggingUtil.uniqueWarn(log, "Failed in executing database command")
            return false
        }else
            sqlIdMap.putAll(map)
        return true
    }

    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}