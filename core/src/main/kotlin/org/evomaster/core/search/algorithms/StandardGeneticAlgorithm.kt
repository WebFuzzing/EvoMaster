package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm

/**
 * An implementation of the Standard Genetic algorithm,
 */
open class StandardGeneticAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {


    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.STANDARD_GA
    }

    override fun searchOnce() {

        val n = config.populationSize

        val nextPop = formTheNextPopulation(population)

        while (nextPop.size < n) {

            val x = tournamentSelection()
            val y = tournamentSelection()
            //x and y are copied

            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(x, y)
            }

            //TODO: check config.fixedRateMutation (preferrably inside the function)
            mutate(x)
            mutate(y)

            nextPop.add(x)
            nextPop.add(y)

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        population.clear()
        population.addAll(nextPop)
    }
}