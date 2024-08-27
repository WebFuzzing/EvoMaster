package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.search.EvaluatedIndividual

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
}