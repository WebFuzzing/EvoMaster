package org.evomaster.core.problem.rest.service


import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.IdMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * take care of calculating/collecting fitness of [RestIndividual]
 */
class RestResourceFitness : AbstractRestFitness<RestIndividual>() {

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var sampler : RestResourceSampler

    @Inject
    private lateinit var dm: ResourceDepManageService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestResourceFitness::class.java)
    }

    /*
        add db check in term of each abstract resource
     */
    override fun doCalculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        //individual.enforceCoherence()

        val cookies = getCookies(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        val sqlIdMap = mutableMapOf<Long, Long>()

        //run the test, one action at a time
        var indexOfAction = 0

        for (call in individual.getResourceCalls()) {

            doInitializingCalls(call.dbActions, sqlIdMap)

            var terminated = false

            for (a in call.actions){

                //TODO handling of inputVariables
                rc.registerNewAction(ActionDto().apply { index = indexOfAction})

                var ok = false

                if (a is RestCallAction) {
                    ok = handleRestCall(a, actionResults, chainState, cookies)
                    /*
                    update creation of resources regarding response status
                     */
                    if (a.verb.run { this == HttpVerb.POST || this == HttpVerb.PUT} && call.status == ResourceStatus.CREATED && (actionResults[indexOfAction] as RestCallResult).getStatusCode().run { this != 201 || this != 200 }){
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

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(
                archive.notCoveredTargets().filter { !IdMapper.isLocal(it) },
                100).toSet()

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        /*
         update dependency regarding executed dto
         */
        if(config.extractSqlExecutionInfo && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.updateResourceTables(individual, dto)

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.seeActions().toMutableList(), actionResults, dto.additionalInfoList)

        if (config.expandRestIndividuals) {
            expandIndividual(individual, dto.additionalInfoList)
        }

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults, enableTracking = config.enableTrackEvaluatedIndividual, trackOperator = if(config.enableTrackEvaluatedIndividual) sampler else null, enableImpact = (config.probOfArchiveMutation > 0.0))

        /*
            TODO when dealing with seeding, might want to extend EvaluatedIndividual
            to keep track of AdditionalInfo
         */
    }

    private fun doInitializingCalls(allDbActions : List<DbAction>, sqlIdMap : MutableMap<Long, Long>) {

        if (allDbActions.isEmpty()) {
            return
        }

        if (allDbActions.none { !it.representExistingData }) {
            /*
                We are going to do an initialization of database only if there
                is data to add.
                Note that current data structure also keeps info on already
                existing data (which of course should not be re-inserted...)
             */
            return
        }


        val dto = DbActionTransformer.transform(allDbActions, sqlIdMap)


        val map = rc.executeDatabaseInsertionsAndGetIdMapping(dto)
        if (map == null) {
            log.warn("Failed in executing database command")
        }else
            sqlIdMap.putAll(map)
    }

    override fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }
}