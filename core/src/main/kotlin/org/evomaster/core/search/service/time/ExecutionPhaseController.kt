package org.evomaster.core.search.service.time

import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.utils.TimeUtils
import javax.inject.Inject

/**
 * Service used to keep track of which phase of the fuzzing we are currently in,
 * and how long we have spent in them.
 *
 * The actual starting/stopping of these phases is done elsewhere (as it depends
 * on the problem type)
 */
class ExecutionPhaseController {

    enum class Phase(
        val timeBoxed: Boolean
    ){
        NOT_STARTED(false),
        SEARCH(false),
        MINIMIZATION(true),
        SECURITY(true),
        ADDITIONAL_ORACLES(true),
        FLAKINESS(true),
        WRITE_OUTPUT(false),
        FINISHED(false)
    }

    @Inject
    private lateinit var config: EMConfig

    private var phase: Phase = Phase.NOT_STARTED

    private var lastPhaseStartMs : Long = 0

    private val durationInSeconds : MutableMap<Phase, Long> = mutableMapOf()

    private val hasTimedOut : MutableSet<Phase> = mutableSetOf()

    fun getCurrentPhase() : Phase{
        return phase
    }

    fun hasPhaseTimedOut(target: Phase) : Boolean {

        if(!target.timeBoxed){
            throw IllegalArgumentException("Cannot query timeout for a non time-boxed phase: $target")
        }

        if(config.stoppingCriterion != EMConfig.StoppingCriterion.TIME){
            //timeouts are defined only when we run base search on time, which is the default
            return false
        }

        if(hasTimedOut.contains(target)){
            return true
        }

        val budget = (config.timeLimitInSeconds() * config.extraPhaseBudgetPercentage).toLong()

        val passed = elapsedSeconds()
        val timeout = passed > budget

        if(timeout){
            LoggingUtil.getInfoLogger().warn("Phase '${phase.name}' has timed out after $budget seconds." +
                    " If you can run the fuzzing for longer, next time you might want to either increase " +
                    "'maxTime' (${config.maxTime})" +
                    "or 'extraPhaseBudgetPercentage' (${config.extraPhaseBudgetPercentage})")
            hasTimedOut.add(phase)
        }

        return timeout
    }

    fun getPhaseDurationInSeconds(target: Phase) : Long {
        return durationInSeconds.getOrDefault(target, -1L)
    }

    fun isInSearch() = phase == Phase.SEARCH

    fun markStartingSearch() {
        if(isRunning()){
            throw IllegalStateException("Illegal state to start new search: $phase")
        }
        startPhase(Phase.SEARCH)
    }

    fun markFinishedSession() {
        startPhase(Phase.FINISHED)
    }

    fun markStartingMinimization() {
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start minimization: $phase")
        }
        startPhase(Phase.MINIMIZATION)
    }

    fun markStartingSecurity(){
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start security: $phase")
        }
        startPhase(Phase.SECURITY)
    }

    fun markStartingFlakiness() {
        if (!isRunning()) {
            throw IllegalStateException("Illegal state to start flakiness detection: $phase")
        }
        startPhase(Phase.FLAKINESS)
    }

    fun markStartingAdditionalOracles(){
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start additional oracles: $phase")
        }
        startPhase(Phase.ADDITIONAL_ORACLES)
    }

    fun markStartingWriteOutput (){
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start write output: $phase")
        }
        startPhase(Phase.WRITE_OUTPUT)
    }

    private fun isRunning() : Boolean{
        return phase != Phase.NOT_STARTED && phase != Phase.FINISHED
    }

    private fun elapsedSeconds() : Long {

        if(phase == Phase.NOT_STARTED){
            throw IllegalStateException("Fuzzing session has not started yet")
        }

        val elapsed = System.currentTimeMillis() - lastPhaseStartMs
        val seconds = elapsed / 1000

        return seconds
    }

    private fun startPhase(newPhase: Phase){

        if(newPhase == Phase.NOT_STARTED){
            throw IllegalStateException("Cannot start a 'not-started' phase")
        }

        if(phase != Phase.NOT_STARTED){
            //starting a new phase will end the current one
            val seconds = elapsedSeconds()
            durationInSeconds[phase] = seconds

            if(!config.avoidNonDeterministicLogs) {
                val time = TimeUtils.getElapsedTime(seconds)
                LoggingUtil.getInfoLogger().info("Phase $phase lasted: $time")
            }
        }

        if(newPhase == Phase.FINISHED){
            LoggingUtil.getInfoLogger().info("Fuzzing session has finished")
        } else {
            LoggingUtil.getInfoLogger().info("Starting phase $newPhase")
        }

        phase = newPhase
        lastPhaseStartMs = System.currentTimeMillis()
    }
}
