package org.evomaster.core.search.service

class ExecutionPhaseController {

    private enum class Phase{
        NOT_STARTED,
        SEARCH,
        SECURITY,
        MINIMIZATION,
        FINISHED
    }

    private var phase: Phase = Phase.NOT_STARTED

    fun isInSearch() = phase == Phase.SEARCH

    fun startSearch() {
        if(phase != Phase.NOT_STARTED && phase != Phase.FINISHED){
            throw IllegalStateException("Illegal state to start new search: $phase")
        }
        phase = Phase.SEARCH
    }

    fun finishSearch() {
        phase = Phase.FINISHED
    }

    fun startMinimization() {
        if(phase == Phase.NOT_STARTED || phase == Phase.FINISHED){
            throw IllegalStateException("Illegal state to start minimization: $phase")
        }
        phase = Phase.MINIMIZATION
    }

    fun startSecurity(){
        phase = Phase.SECURITY
    }
}