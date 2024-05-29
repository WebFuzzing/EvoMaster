package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.genetic.GeneticEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.min

class GeneticAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    private val population: MutableList<GeneticEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.Genetic
    }

    override fun setupBeforeSearch() {
        population.clear()
        initPopulation()
    }

    override fun searchOnce() {
        val n = config.populationSize

        val nextPop: MutableList<GeneticEvalIndividual<T>> = mutableListOf()

        while (nextPop.size < n) {
            val parent1 = selection()
            val parent2 = selection()

            val offspring1 = parent1.copy()
            val offspring2 = parent2.copy()

            if (randomness.nextBoolean(config.xoverProbability)) {
                crossover(offspring1, offspring2)
            }
            mutate(offspring1)
            mutate(offspring2)

            nextPop.add(offspring1)
            nextPop.add(offspring2)

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        population.clear()
        population.addAll(nextPop)
    }

    private fun mutate(individual: GeneticEvalIndividual<T>) {
        //TODO
    }

    private fun selection(): GeneticEvalIndividual<T> {
        // Implement selection logic (e.g., tournament selection)
        val candidate1 = randomness.choose(population)
        val candidate2 = randomness.choose(population)

        return if (candidate1.calculateFitness() > candidate2.calculateFitness()) candidate1.copy() else candidate2.copy()
    }

    private fun crossover(parent1: GeneticEvalIndividual<T>, parent2: GeneticEvalIndividual<T>) {
        if (parent1.genes.isEmpty() || parent2.genes.isEmpty()) return

        // Choose a random crossover point
        val crossoverPoint = randomness.nextInt(min(parent1.genes.size, parent2.genes.size))

        // Swap genes after the crossover point
        for (i in crossoverPoint until parent1.genes.size) {
            if (i < parent2.genes.size) {
                val temp = parent1.genes[i]
                parent1.genes[i] = parent2.genes[i]
                parent2.genes[i] = temp
            }
        }
    }


    private fun initPopulation() {
        val n = config.populationSize

        for (i in 1..n) {
            population.add(sampleIndividual())

            if (!time.shouldContinueSearch()) {
                break
            }
        }
    }

    //TODO
    private fun sampleIndividual(): GeneticEvalIndividual<T> {
        // Generate a new individual with random genes
        // Example: random initialization of genes
        return GeneticEvalIndividual(mutableListOf())
    }
}
