package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.utils.FlakinessInferenceUtil.derive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * it is used to detect flaky tests by checking responses or return value
 * currently, such a detection is performed during post-handling of fuzzing
 */
class FlakinessDetector<T: Individual> {

    companion object{
        private val log : Logger = LoggerFactory.getLogger(FlakinessDetector::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var archive: Archive<T>

    @Inject
    private lateinit var fitness : FitnessFunction<T>

    /**
     * re-execute individuals in archive for identifying flakiness
     */
    fun reexecuteToDetectFlakiness() : Solution<T>{
        /*
            here we rely on extractSolution returning actual references of individuals in the archive, and not copies
         */
        val currentIndividuals = archive.extractSolution().individuals

        LoggingUtil.getInfoLogger().info("Reexecuting all individual ${currentIndividuals.size} for identifying flakiness.")

        currentIndividuals.forEach {

            val ei = fitness.computeWholeAchievedCoverageForPostProcessing(it.individual)
            if(ei == null){
                log.warn("Failed to re-evaluate individual during flakiness analysis.")
            }else{
                checkAndMarkConsistency(ei, it)
            }
        }

        return archive.extractSolution()
    }

    /**
     * This might have side-effects will be applied in the archive
     * compare [inArchive] with [other] to check if the action results are same, the inconsistent info will be saved in [inArchive] evaluated individual
     * @param inArchive the evaluated individual which saves info of flakiness
     */
    fun checkAndMarkConsistency(other: EvaluatedIndividual<T>, inArchive: EvaluatedIndividual<T>){
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
                    handleFlakinessInActionResult(it.result, previousActions[index].result as HttpWsCallResult)
                }

            }
        }
    }

    private fun handleFlakinessInActionResult(
        resultToUpdate: HttpWsCallResult,
        other: HttpWsCallResult
    ) {

        // handle status code
        val rStatus = resultToUpdate.getStatusCode()
        val oStatus = other.getStatusCode()

        if (oStatus != null && rStatus != oStatus) {
            resultToUpdate.setFlakyStatusCode(oStatus)
        }

        // handle body
        val rBody = resultToUpdate.getBody()
        val oBody = other.getBody()

        if (oBody != null) {
            if (rBody != oBody) {
                resultToUpdate.setFlakyBody(oBody)
            } else {
                val normO = derive(oBody)
                if (rBody != normO) {
                    resultToUpdate.setFlakyBody(oBody)
                }
            }
        }

        // handle body type
        val rType = resultToUpdate.getBodyType()
        val oType = other.getBodyType()

        if (oType != null && rType != oType) {
            resultToUpdate.setFlakyBodyType(oType)
        }

        // handle error message
        val rMsg = resultToUpdate.getErrorMessage()
        val oMsg = other.getErrorMessage()

        if (oMsg != null) {
            if (rMsg != oMsg) {
                resultToUpdate.setFlakyErrorMessage(oMsg)
            } else {
                /*
                     there may be chance that the flakiness is not identified with the near execution.
                     However, we use predefined regex to infer potential flaky value for known flaky source,
                     such as UUID, time.
                 */
                val normO = derive(oMsg)
                if (rMsg != normO) {
                    resultToUpdate.setFlakyErrorMessage(oMsg)
                }
            }
        }
    }

}