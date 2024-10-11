package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

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

            val sizeBefore = nextPop.size

            val x = tournamentSelection()
            val y = tournamentSelection()

            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(x, y)
            }

            if(randomness.nextDouble()< config.fixedRateMutation){
                getMutatator().mutate(x.suite.random())
            }

            if(randomness.nextDouble()< config.fixedRateMutation){
                getMutatator().mutate(y.suite.random())
            }

            nextPop.add(x)
            nextPop.add(y)

            //TODO: double check
            assert(nextPop.size == sizeBefore + 2)

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        population.clear()
        population.addAll(nextPop)
    }
}