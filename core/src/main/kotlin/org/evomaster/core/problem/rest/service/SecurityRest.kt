package org.evomaster.core.problem.rest.service

import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution


/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    fun applySecurityPhase() : Solution<RestIndividual>{

        //TODO
        return Solution(mutableListOf(),"","",Termination.NONE, listOf())
    }
}