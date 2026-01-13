package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    fun reexecuteToDetectFlakiness() {

        val currentIndividuals = archive.extractSolution().individuals

        LoggingUtil.getInfoLogger().info("Reexecuting all individual ${currentIndividuals.size} for identifying flakiness.")

        currentIndividuals.mapNotNull {

            val ei = fitness.computeWholeAchievedCoverageForPostProcessing(it.individual)
            if(ei == null){
                log.warn("Failed to re-evaluate individual during flakiness analysis.")
            }else
                checkConsistency(ei, it)

        }

    }

    /**
     * compare [inArchive] with [other] to check if the action results are same, the inconsistent info will be saved in [inArchive] evaluated individual
     * @param inArchive the evaluated individual which saves info of flakiness
     */
    fun checkConsistency(other: EvaluatedIndividual<T>, inArchive: EvaluatedIndividual<T>){
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
                    it.result.setFlakiness(previousActions[index].result as HttpWsCallResult)
                }

            }
        }
    }
}