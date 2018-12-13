package org.evomaster.core.problem.rest.serviceII


import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.search.EvaluatedIndividual
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RestFitnessII : RestFitness<RestIndividualII>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitnessII::class.java)
    }

    override fun doCalculateCoverage(individual: RestIndividualII): EvaluatedIndividual<RestIndividualII>? {

        return super.doCalculateCoverage(individual)
    }
}