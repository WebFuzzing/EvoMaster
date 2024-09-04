package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

object DetectedFaultUtils {

    fun getDetectedFaultCategories(ei: EvaluatedIndividual<*>) : Set<FaultCategory> {

        if(ei.individual !is EnterpriseIndividual){
            throw IllegalArgumentException("Not an Enterprise Individual")
        }

        return ei.seeResults()
            .filterIsInstance<EnterpriseActionResult>()
            .flatMap { it.getFaults() }
            .map { it.category }
            .toSet()
    }

    fun getDetectedFaultCategories(solution: Solution<*>) : Set<FaultCategory> {

        return solution.individuals
            .flatMap { getDetectedFaultCategories(it) }
            .toSet()
    }

}