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

    var evaluatedActions = 0
        private set

    var searchStarted = false
        private set

    var lastActionImprovement = -1
        private set

    private var startTime = 0L

    private val listeners = mutableListOf<SearchListener>()


    fun startSearch(){
        searchStarted = true
        startTime = System.currentTimeMillis()
    }

    fun addListener(listener: SearchListener){
        listeners.add(listener)
    }

    fun newIndividualEvaluation() {
        evaluatedIndividuals++
    }

    fun newActionEvaluation(n: Int = 1) {
        evaluatedActions += n
        listeners.forEach{it.newActionEvaluated()}
    }

    fun newCoveredTarget(){
        newActionImprovement()
    }

    fun newActionImprovement(){
        lastActionImprovement = evaluatedActions
    }


    fun getElapsedSeconds() : Int{
        if(!searchStarted){
            return 0
        }

        return ((System.currentTimeMillis() - startTime) / 1000.0).toInt()
    }

    fun shouldContinueSearch(): Boolean{

        if(configuration.stoppingCriterion.equals(
                EMConfig.StoppingCriterion.FITNESS_EVALUATIONS))    {

            return evaluatedActions < configuration.maxActionEvaluations
        }

        return false //TODO
    }

    /**
     * Return how much percentage `[0,1]` of search budget has been used so far
     */
    fun percentageUsedBudget() : Double{

        if(configuration.stoppingCriterion.equals(
                EMConfig.StoppingCriterion.FITNESS_EVALUATIONS))    {

            return evaluatedActions.toDouble() / configuration.maxActionEvaluations.toDouble()
        } else {
            return -1.0; //TODO
        }
    }
}