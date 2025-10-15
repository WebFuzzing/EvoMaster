package org.evomaster.core.search.algorithms.observer

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * Observer for GA internal events used primarily for testing/telemetry.
 * Default methods are no-ops so listeners can implement only what they need.
 */
interface GAObserver<T : Individual> {
    /** Called at the start of a generation (one call to searchOnce). */
    fun onGenerationStart() {}

    /** Called at the start of a step inside a generation. */
    fun onStepStart() {}

    /** Called when one parent is selected. */
    fun onSelection(sel: WtsEvalIndividual<T>) {}
    /** Called immediately after crossover is applied to [x] and [y]. */
    fun onCrossover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {}

    /** Called immediately after mutation is applied to [wts]. */
    fun onMutation(wts: WtsEvalIndividual<T>) {}

    /**
     * Called at the end of a generation (one call to searchOnce), with a snapshot of the final population.
     */
    fun onGenerationEnd(population: List<WtsEvalIndividual<T>>) {}

    /**
     * Called at the end of a step inside a generation.
     */
    fun onStepEnd() {}
}


