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

        return percentageUsedBudget() < 1.0
    }

    /**
     * Return how much percentage `[0,1]` of search budget has been used so far
     */
    fun percentageUsedBudget() : Double{

        return when(configuration.stoppingCriterion){
            EMConfig.StoppingCriterion.FITNESS_EVALUATIONS ->
                evaluatedActions.toDouble() / configuration.maxActionEvaluations.toDouble()

            EMConfig.StoppingCriterion.TIME ->
                (System.currentTimeMillis() - startTime).toDouble() /
                        (configuration.maxTimeInSeconds.toDouble() * 1000.0)

            else ->
                throw IllegalStateException("Not supported stopping criterion")
        }
    }
}