package org.evomaster.core.problem.rest

import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult

object RestSecurityOracle {


    /**
     * Check if the last 3 actions represent the following scenario:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     *
     * if so, a new "fault" target is added to the fitness function
     *
     * @return the name chosen for the found fault, if any. null otherwise
     */
    fun handleForbiddenDelete(individual: RestIndividual,
                              actionResults: List<ActionResult>,
                              fv: FitnessValue
    ) : String?{

        if(individual.sampleType != SampleType.SECURITY){
            throw IllegalArgumentException("We verify security properties only on tests constructed to check them")
        }

        //TODO

        return null
    }

}