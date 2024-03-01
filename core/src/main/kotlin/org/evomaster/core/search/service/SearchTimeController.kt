package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.utils.IncrementalAverage
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.ceil

/**
 * Class used to keep track of passing of time during the search.
 * This is needed for deciding when to stop the search, and for
 * other time-related properties like adaptive parameter control.
 */
class SearchTimeController {

    @Inject
    private lateinit var configuration: EMConfig

    companion object{
        private val log = LoggerFactory.getLogger(SearchTimeController::class.java)

        /**
         * From https://proandroiddev.com/measuring-execution-times-in-kotlin-460a0285e5ea
         */
        inline fun <T> measureTimeMillis(loggingFunction: (Long, T) -> Unit,
                                         function: () -> T): T {

            val startTime = System.currentTimeMillis()
            val result: T = function.invoke()
            loggingFunction.invoke(System.currentTimeMillis() - startTime, result)

            return result
        }
    }


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

    var lastActionTimestamp = 0L
        private set

    /**
     * The SUT should avoid sending HTTP requests with "Connection: close", as it puts strains on the OS,
     * possibly running out of available ports when running experiments
     */
    var connectionCloseRequest = 0
        private set

    var actionWhenLastConnectionCloseRequest = -1
        private set

    private var startTime = 0L

    /**
     * Once the search is finished, we do not want to keep recording new events.
     * The problem is with phases after the search, like minimization and security, which
     * might end up calling methods here through the archive
     */
    private var recording = true

    /**
     * Keeping track of the latest N test executions.
     * Time expressed in ms (Long).
     * Also keeping track of number of actions (Int)
     */
    private val executedIndividualTime : Queue<Pair<Long,Int>> = ArrayDeque(100)

    private val listeners = mutableListOf<SearchListener>()

    val averageTestTimeMs = IncrementalAverage()

    val averageActionTimeMs = IncrementalAverage()

    val averageOverheadMsBetweenTests = IncrementalAverage()

    val averageResetSUTTimeMs = IncrementalAverage()

    val averageByteOverheadTestResultsAll = IncrementalAverage()

    val averageByteOverheadTestResultsSubset = IncrementalAverage()

    val averageOverheadMsTestResultsSubset = IncrementalAverage()


    fun doStopRecording(){
        recording = false
    }

    /**
     * Make sure we do not make too many requests in a short amount of time, to avoid
     * possible DoS attacks.
     */
    fun waitForRateLimiter(){
        if(configuration.ratePerMinute <=0){
            //nothing to do
            return
        }

        val now = System.currentTimeMillis()
        val passed = now - lastActionTimestamp

        val delta = ceil((60.0 * 1000.0) / configuration.ratePerMinute).toLong()

        if(passed >= delta){
            //already slow enough, nothing to do
            lastActionTimestamp = now
            return
        }

        val toWait = delta - passed
        Thread.sleep(toWait)
        lastActionTimestamp = System.currentTimeMillis()
    }

    fun startSearch(){
        recording = true
        searchStarted = true
        startTime = System.currentTimeMillis()
    }

    fun addListener(listener: SearchListener){
        listeners.add(listener)
    }

    fun reportConnectionCloseRequest(httpStatus: Int){

        if(!recording) return

        connectionCloseRequest++
        //evaluatedActions is updated at the end of test case
        //assert(evaluatedActions > actionWhenLastConnectionCloseRequest)

        val total = "$connectionCloseRequest/$evaluatedActions"
        val sinceLast =  if(actionWhenLastConnectionCloseRequest < 1){
            "0/0"
        } else {
            "1/${evaluatedActions-actionWhenLastConnectionCloseRequest}"
        }

        actionWhenLastConnectionCloseRequest = evaluatedActions

        LoggingUtil.uniqueWarn(log, "The SUT sent a 'Connection: close' HTTP header. This should be avoided, if possible")
        log.debug("SUT requested a 'Connection: close' in a HTTP response. Ratio: total=$total , since-last=$sinceLast, HTTP=$httpStatus")
    }

    fun reportExecutedIndividualTime(ms: Long, nActions: Int){

        if(!recording) return

        //this is for last 100 tests, displayed live during the search in the console
        executedIndividualTime.add(Pair(ms, nActions))
        if(executedIndividualTime.size > 100){
            executedIndividualTime.poll()
        }

        // for all tests evaluated so far
        averageTestTimeMs.addValue(ms)
        averageActionTimeMs.addValue(ms.toDouble() / nActions.toDouble())
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
        if(!recording) return
        evaluatedIndividuals++
    }

    fun newIndividualsWithSqlFailedWhere(){
        if(!recording) return
        individualsWithSqlFailedWhere++
    }

    fun newActionEvaluation(n: Int = 1) {
        if(!recording) return
        evaluatedActions += n
        listeners.forEach{it.newActionEvaluated()}
    }

    fun newCoveredTarget(){
        if(!recording) return
        newActionImprovement()
    }

    fun newActionImprovement(){
        if(!recording) return
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