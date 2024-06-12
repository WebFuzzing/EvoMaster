package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.service.SearchAlgorithm


class GeneticAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.Genetic
    }

    override fun setupBeforeSearch() {
        population.clear()
        initPopulation()
    }

    override fun searchOnce() {
        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        while (nextPop.size < n) {
            val x = selection()
            val y = selection()

            crossover(x, y)
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

    private fun mutate(individual: WtsEvalIndividual<T>) {
        // Randomly select a test case from the suite
        val index = randomness.nextInt(individual.suite.size)
        val testCase = individual.suite[index]

        mutateTestCase(testCase)
    }

    private fun mutateTestCase(testCase: EvaluatedIndividual<T>) {
        val genes = testCase.individual.seeGenes()

        // Randomly select a gene to mutate
        val geneIndex = randomness.nextInt(genes.size)
        val geneToMutate = genes[geneIndex]

        // For demonstration, we toggle a boolean value if the gene is of type BooleanGene
        if (geneToMutate is BooleanGene) {
            val oldValue = geneToMutate.value
            val newValue = !oldValue
            geneToMutate.value = newValue
        }
        if (geneToMutate is DoubleGene) {

            if(geneToMutate.value<Double.MAX_VALUE){
                geneToMutate.value += 1
            }
            else{
                geneToMutate.value -= 1
            }
        }
    }

    private fun selection(): WtsEvalIndividual<T> {
        val totalFitness = population.sumOf { it.calculateCombinedFitness() }
        var randomNumber = randomness.nextDouble() * totalFitness
        var accumulatedFitness = 0.0

        for (individual in population) {
            accumulatedFitness += individual.calculateCombinedFitness()
            if (accumulatedFitness >= randomNumber) {
                return individual
            }
        }

        return population.last()
    }

    private fun crossover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {
        val n = x.suite.size
        val splitPoint = randomness.nextInt(n)

        for (i in 0 until splitPoint) {
            val temp = x.suite[i]
            x.suite[i] = y.suite[i]
            y.suite[i] = temp
        }
    }


    private fun initPopulation() {
        val n = config.populationSize
        for (i in 1..n) {
            population.add(sampleSuite())
            if (!time.shouldContinueSearch()) {
                break
            }
        }
    }

    private fun sampleSuite(): WtsEvalIndividual<T> {

        val n = 1 + randomness.nextInt(config.maxSearchSuiteSize)

        val wts = WtsEvalIndividual<T>(mutableListOf())

        for (i in 1..n) {
            ff.calculateCoverage(sampler.sample())?.run {
                archive.addIfNeeded(this)
                wts.suite.add(this)
            }

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        return wts
    }



}
