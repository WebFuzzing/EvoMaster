package org.evomaster.core.search.service

import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import javax.inject.Inject

class ExecutionPhaseController {

    enum class Phase{
        NOT_STARTED,
        SEARCH,
        MINIMIZATION,
        SECURITY,
        HTTP_ORACLES,
        //FLAKINESS, //TODO
        WRITE_OUTPUT,
        FINISHED
    }

    @Inject
    private lateinit var config: EMConfig

    private var phase: Phase = Phase.NOT_STARTED

    private var lastPhaseStartMs : Long = 0

    private val durationInSeconds : MutableMap<Phase, Long> = mutableMapOf()


    fun getPhaseDurationInSeconds(target: Phase) : Long {
        return durationInSeconds.getOrDefault(target, -1L)
    }

    fun isInSearch() = phase == Phase.SEARCH

    fun startSearch() {
        if(isRunning()){
            throw IllegalStateException("Illegal state to start new search: $phase")
        }
        startPhase(Phase.SEARCH)
    }

    fun finishSearch() {
        startPhase(Phase.FINISHED)
    }

    fun startMinimization() {
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start minimization: $phase")
        }
        startPhase(Phase.MINIMIZATION)
    }

    fun startSecurity(){
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start security: $phase")
        }
        startPhase(Phase.SECURITY)
    }

    fun startHttpOracles(){
        if(!isRunning()) {
            throw IllegalStateException("Illegal state to start http oracles: $phase")
        }
        startPhase(Phase.HTTP_ORACLES)
    }

    fun startWriteOutput (){
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
                val time = SearchTimeController.getElapsedTime(seconds)
                LoggingUtil.getInfoLogger().info("Phase $phase lasted: $time")
            }
        }

        phase = newPhase
        lastPhaseStartMs = System.currentTimeMillis()
    }
}