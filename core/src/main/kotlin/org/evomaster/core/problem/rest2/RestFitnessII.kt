package org.evomaster.core.problem.rest.serviceII


import com.google.inject.Inject
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RestFitnessII : RestFitness<RestIndividualII>() {

    @Inject
    private lateinit var rm : ResourceManageService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitnessII::class.java)
    }

    /*
        add db check in term of each abstract resource
     */
    override fun doCalculateCoverage(individual: RestIndividualII): EvaluatedIndividual<RestIndividualII>? {

        rc.resetSUT()

        //doInitializingActions(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        var indexOfAction = 0
        for (call in individual.getResourceCalls()) {

            if(call.doesCompareDB)
                rm.snapshotDB()

            doInitializingCalls(call)

            for (a in call.actions){
                rc.registerNewAction(indexOfAction)

                var ok = false

                if (a is RestCallAction) {
                    ok = handleRestCall(a, actionResults, chainState)
                } else {
                    throw IllegalStateException("Cannot handle: ${a.javaClass}")
                }

                if (!ok) {
                    break
                }
                indexOfAction++
            }

            if(call.doesCompareDB){
                /*
                    TODO Man: 1) check whether data is changed regarding actions. Note that call.dbaction saved previous data of row of all columned
                              2) if only check only one row regarding pks and its related table instead of all tables
                 */
                rm.compareDB(call)
            }

        }

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(archive.notCoveredTargets(), 100)

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.actions, actionResults)

        expandIndividual(individual, dto.additionalInfoList)

        return if(config.enableTrackEvaluatedIndividual)
            EvaluatedIndividual(fv, individual.copy() as RestIndividualII, actionResults, null, mutableListOf(), mutableListOf())
        else EvaluatedIndividual(fv, individual.copy() as RestIndividualII, actionResults)

        /*
            TODO when dealing with seeding, might want to extend EvaluatedIndividual
            to keep track of AdditionalInfo
         */
    }

    fun doInitializingCalls(calls: RestResourceCalls) {

        if (calls.dbActions.isEmpty()) {
            return
        }

        val dto = DbActionTransformer.transform(calls.dbActions)

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            log.warn("Failed in executing database command")
        }
    }
}