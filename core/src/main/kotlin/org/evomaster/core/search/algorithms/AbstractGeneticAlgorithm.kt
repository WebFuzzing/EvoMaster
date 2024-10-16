package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm

abstract class AbstractGeneticAlgorithm<T>: SearchAlgorithm<T>() where T : Individual {

    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun setupBeforeSearch() {
        population.clear()

        initPopulation()
    }

    protected open fun initPopulation() {

        val n = config.populationSize

        for (i in 1..n) {
            population.add(sampleSuite())

            if (!time.shouldContinueSearch()) {
                break
            }
        }
    }

    protected fun formTheNextPopulation(population: MutableList<WtsEvalIndividual<T>>): MutableList<WtsEvalIndividual<T>> {

        val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        if (config.elitesCount > 0 && population.isNotEmpty()) {
            var sortedPopulation = population.sortedByDescending { it.calculateCombinedFitness() }

            var elites = sortedPopulation.take(config.elitesCount)

            nextPop.addAll(elites)
        }

        return nextPop
    }

    protected fun mutate(wts: WtsEvalIndividual<T>) {

        val op = randomness.choose(listOf("del", "add", "mod"))
        val n = wts.suite.size
        when (op) {
            "del" -> if (n > 1) {
                val i = randomness.nextInt(n)
                wts.suite.removeAt(i)
            }
            "add" -> if (n < config.maxSearchSuiteSize) {
                ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
                    archive.addIfNeeded(this)
                    wts.suite.add(this)
                }
            }
            "mod" -> {
                val i = randomness.nextInt(n)
                val ind = wts.suite[i]

                getMutatator().mutateAndSave(ind, archive)
                    ?.let { wts.suite[i] = it }
            }
        }
    }

    protected fun xover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {

        val nx = x.suite.size
        val ny = y.suite.size

        val splitPoint = randomness.nextInt(Math.min(nx, ny))

        (0..splitPoint).forEach {
            val k = x.suite[it]
            x.suite[it] = y.suite[it]
            y.suite[it] = k
        }
    }

    protected fun tournamentSelection(): WtsEvalIndividual<T>{
        val selectedIndividuals = randomness.choose(population, config.tournamentSize)
        val bestIndividual = selectedIndividuals.maxByOrNull { it.calculateCombinedFitness() }
        return bestIndividual ?: randomness.choose(population)
    }

    private fun sampleSuite(): WtsEvalIndividual<T> {

        val n = 1 + randomness.nextInt(config.maxSearchSuiteSize)

        val suite = WtsEvalIndividual<T>(mutableListOf())

        for (i in 1..n) {
            ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
                archive.addIfNeeded(this)
                suite.suite.add(this)
            }

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        return suite
    }
}