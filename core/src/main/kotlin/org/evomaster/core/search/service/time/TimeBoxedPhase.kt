package org.evomaster.core.search.service.time

/**
 * A service representing one phase of fuzzing that is time-boxed.
 * This means that the phase should not run more than a certain amount of time.
 * If such limit is reached, the phase is prematurely stopped.
 * Ideally, most of the time we would expect the phase to finish way earlier than the timeout.
 *
 * Note: this is quite different from typical "search" phase, where we usually run for all
 * the available budget
 */
interface TimeBoxedPhase {

    fun applyPhase()

    fun hasPhaseTimedOut(): Boolean
}
