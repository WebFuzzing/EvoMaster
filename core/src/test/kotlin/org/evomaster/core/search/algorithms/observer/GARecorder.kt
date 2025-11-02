package org.evomaster.core.search.algorithms.observer

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/** Test utility observer that records GA events. */
class GARecorder<T : Individual> : GAObserver<T> {
    val selections = mutableListOf<WtsEvalIndividual<T>>()
    val xoCalls = mutableListOf<Pair<WtsEvalIndividual<T>, WtsEvalIndividual<T>>>()
    val mutated = mutableListOf<WtsEvalIndividual<T>>()
    val bestFitnessPerGeneration = mutableListOf<Double>()

    override fun onSelection(sel: WtsEvalIndividual<T>) {
        selections.add(sel)
    }

    override fun onCrossover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {
        xoCalls.add(x to y)
    }

    override fun onMutation(wts: WtsEvalIndividual<T>) {
        mutated.add(wts)
    }

    override fun onGenerationEnd(population: List<WtsEvalIndividual<T>>, bestScore: Double) {
        bestFitnessPerGeneration.add(bestScore)
    }
}


