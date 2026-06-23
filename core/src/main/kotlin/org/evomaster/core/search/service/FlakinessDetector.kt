package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.evomaster.core.search.service.time.TimeBoxedPhase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * it is used to detect flaky tests by checking responses or return value
 * currently, such a detection is performed during post-handling of fuzzing
 */
class FlakinessDetector<T: Individual> : TimeBoxedPhase {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FlakinessDetector::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var archive: Archive<T>

    @Inject
    private lateinit var fitness : FitnessFunction<T>

    @Inject
    private lateinit var epc: ExecutionPhaseController

    override fun applyPhase() {
        if (config.enableStaticFlakyInference) {
            inferStaticFlakiness()
        }
        if (config.execNumForDetectFlakiness > 0) {
            reexecuteToDetectFlakiness(config.execNumForDetectFlakiness)
        }
    }

    override fun hasPhaseTimedOut(): Boolean {
        return epc.hasPhaseTimedOut(ExecutionPhaseController.Phase.SECURITY)
    }

    /**
     * re-execute individuals in archive for identifying flakiness
     */
    fun reexecuteToDetectFlakiness(execNum : Int) : Solution<T>{
        /*
            here we rely on extractSolution returning actual references of individuals in the archive, and not copies
         */
        val currentIndividuals = archive.extractSolution().individuals

        LoggingUtil.getInfoLogger().info("Reexecuting all individual ${currentIndividuals.size} for identifying flakiness.")

        for(ci in currentIndividuals){

            if(hasPhaseTimedOut()) break

            for (execIndex in 1..execNum){
                val ei = fitness.computeWholeAchievedCoverageForPostProcessing(ci.individual)
                if(ei == null){
                    log.warn("Failed to re-evaluate individual at index ($execIndex) during flakiness analysis.")
                }else{
                    checkAndMarkConsistency(ei, ci, execIndex)
                }
            }
        }

        return archive.extractSolution()
    }

    /**
     * Infer potential flaky response values without re-executing the SUT.
     */
    fun inferStaticFlakiness(): Solution<T> {
        val currentIndividuals = archive.extractSolution().individuals

        LoggingUtil.getInfoLogger().info("Inferring static flakiness for ${currentIndividuals.size} individuals.")

        for (ci in currentIndividuals) {
            if(hasPhaseTimedOut()) break

            ci.evaluatedMainActions()
                .filter { it.action is HttpWsAction && it.result is HttpWsCallResult }
                .forEach {
                    (it.result as HttpWsCallResult).recordStaticFlakyInference()
                }
        }

        return archive.extractSolution()
    }

    /**
     * This might have side-effects will be applied in the archive
     * compare [inArchive] with [other] to check if the action results are same, the inconsistent info will be saved in [inArchive] evaluated individual
     * @param inArchive the evaluated individual which saves info of flakiness
     * @param indexExecN the index of execution times, eg, 1st, 2nd
     */
    fun checkAndMarkConsistency(other: EvaluatedIndividual<T>, inArchive: EvaluatedIndividual<T>, indexExecN: Int){
        val previousActions = other.evaluatedMainActions()
        val currentActions = inArchive.evaluatedMainActions()

        if(previousActions.size != currentActions.size){
            log.warn("Mismatch between number of actions in re-executed individual." +
                    " Previous =${previousActions.size}, Current =${currentActions.size}")
            return
        }

        currentActions.forEachIndexed { index, it ->
            val action = it.action
            if(action is HttpWsAction){
                if (it.result is HttpWsCallResult){
                    handleFlakinessInActionResult(it.result, previousActions[index].result as HttpWsCallResult, indexExecN)
                }

            }
        }
    }

    private fun handleFlakinessInActionResult(
        resultToUpdate: HttpWsCallResult,
        other: HttpWsCallResult,
        indexExecN: Int
    ) {
        resultToUpdate.recordFlakyObservation(other, indexExecN)
    }

}
