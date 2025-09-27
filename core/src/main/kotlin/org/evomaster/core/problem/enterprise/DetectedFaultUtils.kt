package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionResult

object DetectedFaultUtils {

    fun getDetectedFaults(ei: EvaluatedIndividual<*>) : Set<DetectedFault> {

        if(ei.individual !is EnterpriseIndividual){
            throw IllegalArgumentException("Not an Enterprise Individual")
        }

        return ei.seeResults()
            .filterIsInstance<EnterpriseActionResult>()
            .flatMap { it.getFaults() }
            .toSet()
    }

    fun getDetectedFaults(actionResults: List<ActionResult>) : Set<DetectedFault> {

        return actionResults
            .filterIsInstance<EnterpriseActionResult>()
            .flatMap { it.getFaults() }
            .toSet()
    }

    fun getDetectedFaultCategories(actionResults: List<ActionResult>) : Set<FaultCategory> {
        return getDetectedFaults(actionResults).map { it.category }.toSet()
    }

    fun getDetectedFaultCategories(ei: EvaluatedIndividual<*>) : Set<FaultCategory> {
        return getDetectedFaults(ei).map { it.category }.toSet()
    }


    fun getDetectedFaults(solution: Solution<*>) : Set<DetectedFault> {

        return solution.individuals
            .flatMap { getDetectedFaults(it) }
            .toSet()
    }

    fun getDetectedFaultCategories(solution: Solution<*>) : Set<FaultCategory> {

        return solution.individuals
            .flatMap { getDetectedFaultCategories(it) }
            .toSet()
    }

    fun verifyExcludedCategories(actionResults: List<ActionResult>, excludedCategories: List<FaultCategory>) : Boolean {
        val detected = getDetectedFaultCategories(actionResults)
        return excludedCategories.intersect(detected).isEmpty()
    }

}
