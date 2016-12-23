package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig

/**
 * Class used to keep track of passing of time during the search.
 * This is needed for deciding when to stop the search, and for
 * other time-related properties like adaptive parameter control.
 */
class SearchTimeController {

    @Inject
    private lateinit var configuration: EMConfig


    var evaluatedIndividuals = 0
        private set

    var searchStarted = false
        private set


    fun startSearch(){
        searchStarted = true
    }

    fun newEvaluation(){
        evaluatedIndividuals++
    }


    fun shouldContinueSearch(): Boolean{

        if(configuration.stoppingCriterion.equals(
                EMConfig.StoppingCriterion.FITNESS_EVALUATIONS))    {

            return evaluatedIndividuals < configuration.maxFitnessEvaluations
        }

        return false //TODO
    }
}