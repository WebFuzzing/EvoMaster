package org.evomaster.core.search.algorithms.genetic

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

class GeneticEvalIndividual<T>(val genes: MutableList<EvaluatedIndividual<T>>)
        where T : Individual {

    fun copy(): GeneticEvalIndividual<T> {
        return GeneticEvalIndividual(genes.map { it.copy() }.toMutableList())
    }

    fun calculateFitness(): Double {
        if (genes.isEmpty()) {
            return 0.0
        }

        val fitnessValue = genes.first().fitness.copy()
        genes.forEach { gene -> fitnessValue.merge(gene.fitness) }

        return fitnessValue.computeFitnessScore()
    }

    fun size(): Int {
        return genes.map { it.individual.size() }.sum()
    }
}
