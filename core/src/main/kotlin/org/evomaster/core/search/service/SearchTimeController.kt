package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import java.util.*

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

    var individualsWithSqlFailedWhere = 0
        private set

    var evaluatedActions = 0
        private set

    var searchStarted = false
        private set

    var lastActionImprovement = -1
        private set

    private var startTime = 0L

    private val executedIndividualTime : Queue<Pair<Long,Int>> = ArrayDeque(100)

    private val listeners = mutableListOf<SearchListener>()

    fun startSearch(){
        searchStarted = true
        startTime = System.currentTimeMillis()
    }

    fun addListener(listener: SearchListener){
        listeners.add(listener)
    }

    fun reportExecutedIndividualTime(ms: Long, nActions: Int){

        executedIndividualTime.add(Pair(ms, nActions))
        if(executedIndividualTime.size > 100){
            executedIndividualTime.poll()
        }
    }

    /**
     * From https://proandroiddev.com/measuring-execution-times-in-kotlin-460a0285e5ea
     */
    inline fun <T> measureTimeMillis(loggingFunction: (Long) -> Unit,
                                    function: () -> T): T {

        val startTime = System.currentTimeMillis()
        val result: T = function.invoke()
        loggingFunction.invoke(System.currentTimeMillis() - startTime)

        return result
    }

    fun computeExecutedIndividualTimeStatistics() : Pair<Double,Double>{

        if(executedIndividualTime.isEmpty()){
            return Pair(0.0, 0.0)
        }

        return Pair(
                executedIndividualTime.map{it.first}.average(),
                executedIndividualTime.map{it.second}.average()
        )
    }

    fun newIndividualEvaluation() {
        evaluatedIndividuals++
    }

    fun newIndividualsWithSqlFailedWhere(){
        individualsWithSqlFailedWhere++
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

    fun getElapsedTime() : String{

        val seconds = getElapsedSeconds()

        val minutes = seconds / 60.0

        val hours = minutes / 60.0

        val ps = "%d".format(seconds % 60)
        val pm = "%d".format(minutes.toInt() % 60)
        val ph = "%d".format(hours.toInt())

        return "${ph}h ${pm}m ${ps}s"
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
                        (configuration.timeLimitInSeconds().toDouble() * 1000.0)

            else ->
                throw IllegalStateException("Not supported stopping criterion")
        }
    }

    fun getStartTime() : Long {
        return startTime
    }

    fun neededBudget() : String{

        if(evaluatedActions <=0 || lastActionImprovement <= 0){
            return "100%"
        } else {
            val percentage = ((lastActionImprovement / evaluatedActions.toDouble()) * 100.0).toInt()
            return "$percentage%"
        }
    }
}