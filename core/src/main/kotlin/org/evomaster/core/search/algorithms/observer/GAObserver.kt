package org.evomaster.core.search.algorithms.observer

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * Observer for GA internal events used primarily for testing/telemetry.
 * Default methods are no-ops so listeners can implement only what they need.
 */
interface GAObserver<T : Individual> {
    /** Called when one parent is selected. */
    fun onSelection(sel: WtsEvalIndividual<T>) {}
    /** Called immediately after crossover is applied to [x] and [y]. */
    fun onCrossover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {}

    /** Called immediately after mutation is applied to [wts]. */
    fun onMutation(wts: WtsEvalIndividual<T>) {}
}


