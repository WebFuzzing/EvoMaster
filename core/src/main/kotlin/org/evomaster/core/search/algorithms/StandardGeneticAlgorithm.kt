package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm

/**
 * An implementation of the Standard Genetic algorithm,
 */
open class StandardGeneticAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {


    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.STANDARD_GA
    }

    override fun setupBeforeSearch() {
        population.clear()

        initPopulation()
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

    override fun searchOnce() {

        val n = config.populationSize

        val nextPop = formTheNextPopulation(population)

        while (nextPop.size < n) {

            val x = selection()
            val y = selection()
            //x and y are copied

            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(x, y)
            }

            //TODO: check config.fixedRateMutation
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

    protected fun selection(): WtsEvalIndividual<T> {

        val x = randomness.choose(population)
        val y = randomness.choose(population)

        val dif = x.calculateCombinedFitness() - y.calculateCombinedFitness()
        val delta = 0.001

        return when {
            dif > delta -> x.copy()
            dif < -delta -> y.copy()
            else -> when {
                x.size() <= y.size() -> x.copy()
                else -> y.copy()
            }
        }
    }


    private fun xover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {

        val nx = x.suite.size
        val ny = y.suite.size

        val splitPoint = randomness.nextInt(Math.min(nx, ny))

        (0..splitPoint).forEach {
            val k = x.suite[it]
            x.suite[it] = y.suite[it]
            y.suite[it] = k
        }
    }

    protected fun initPopulation() {

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
            ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
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