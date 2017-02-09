package org.evomaster.core.search.algorithms.wts

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual


class WtsEvalIndividual<T>(val suite: MutableList<EvaluatedIndividual<T>>)
where T : Individual {

    fun copy(): WtsEvalIndividual<T> {
        return WtsEvalIndividual(suite.map { w -> w.copy() }.toMutableList())
    }

    fun calculateCombinedFitness(): Double{

        if(suite.isEmpty()){
            return 0.0;
        }

        val fv = suite.first().fitness.copy()

        suite.forEach { i -> fv.merge(i.fitness) }

        return fv.computeFitnessScore()
    }

    fun size() : Int {
        return suite.map { e -> e.individual.size() }.sum()
    }
}