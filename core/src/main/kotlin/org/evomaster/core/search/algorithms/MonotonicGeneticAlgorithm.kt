package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max

/**
 * An implementation of the Monotonic GA
 */
class MonotonicGeneticAlgorithm<T> : StandardGeneticAlgorithm<T>() where T : Individual {


    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.MonotonicGA
    }

    override fun setupBeforeSearch() {
        population.clear()

        initPopulation()
    }

    override fun searchOnce() {


        val n = config.populationSize


        //new generation

        val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        while (nextPop.size < n) {
            val p1 = selection()
            val p2 = selection()

            val o1: WtsEvalIndividual<T>
            val o2: WtsEvalIndividual<T>

            if (randomness.nextBoolean(config.xoverProbability)) {
                val offsprings = xover(p1, p2)
                o1 = offsprings.first
                o2 = offsprings.second
            } else {
                o1 = p1
                o2 = p2
            }

            mutate(o1)
            mutate(o2)

            if (max(o1.calculateCombinedFitness(), o2.calculateCombinedFitness()) > max(
                    p1.calculateCombinedFitness(),
                    p2.calculateCombinedFitness()
                )
            ) {
                nextPop.add(o1)
                nextPop.add(o2)
            } else {
                nextPop.add(p1)
                nextPop.add(p2)
            }

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        population.clear()
        population.addAll(nextPop)
    }


    private fun xover(
        x: WtsEvalIndividual<T>,
        y: WtsEvalIndividual<T>
    ): Pair<WtsEvalIndividual<T>, WtsEvalIndividual<T>> {
        val nx = x.suite.size
        val ny = y.suite.size

        val splitPoint = randomness.nextInt(Math.min(nx, ny))

        // Create copies of the parents
        val offspring1 = x.copy()
        val offspring2 = y.copy()

        (0..splitPoint).forEach {
            val temp = offspring1.suite[it]
            offspring1.suite[it] = offspring2.suite[it]
            offspring2.suite[it] = temp
        }

        return Pair(offspring1, offspring2)
    }
}