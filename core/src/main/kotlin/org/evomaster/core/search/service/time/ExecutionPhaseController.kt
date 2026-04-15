package org.evomaster.core.search.service.time

import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.utils.TimeUtils
import javax.inject.Inject

/**
 * Service used to keep track of which phase of the fuzzing we are currently in,
 * and how long we have spent in them.
 */
class ExecutionPhaseController {

    enum class Phase{
        NOT_STARTED,
        SEARCH,
        MINIMIZATION,
        SECURITY,
        ADDITIONAL_ORACLES,
        FLAKINESS,
        WRITE_OUTPUT,
        FINISHED
    }

    @Inject
    private lateinit var config: EMConfig

    private var phase: Phase = Phase.NOT_STARTED

    private var lastPhaseStartMs : Long = 0

    private val durationInSeconds : MutableMap<Phase, Long> = mutableMapOf()

    fun getCurrentPhase() : Phase{
        return phase
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

    private fun startPhase(newPhase: Phase){

        if(phase != Phase.NOT_STARTED){
            //starting a new phase will end the current one
            val elapsed = System.currentTimeMillis() - lastPhaseStartMs
            val seconds = elapsed / 1000
            durationInSeconds[phase] = seconds

            if(!config.avoidNonDeterministicLogs) {
                val time = TimeUtils.getElapsedTime(seconds)
                LoggingUtil.getInfoLogger().info("Phase $phase lasted: $time")
            }
        }

        phase = newPhase
        lastPhaseStartMs = System.currentTimeMillis()
    }
}
