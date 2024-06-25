package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max

/**
 * An implementation of the Whole Test Suite (WTS) algorithm,
 * as used in EvoSuite.
 * <br>
 * Each individual is a set of test cases (ie a test suite),
 * and we use a population algorithm as Genetic Algorithm (GA).
 * But still use an archive to do not lose best individuals.
 * <br>
 * Note: unless some unknown side-effect, or bug, WTS would be
 * worse than MIO on average.
 * This implementation was written mainly to run experiments on comparisons
 * of search algorithms, and not really something to
 * use regularly in EvoMaster
 */
class MonotonicGeneticAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {


    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.WTS
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

    private fun mutate(wts: WtsEvalIndividual<T>) {

        val op = randomness.choose(listOf("del", "add", "mod"))
        val n = wts.suite.size
        when (op) {
            "del" -> if (n > 1) {
                val i = randomness.nextInt(n)
                wts.suite.removeAt(i)
            }

            "add" -> if (n < config.maxSearchSuiteSize) {
                ff.calculateCoverage(sampler.sample())?.run {
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

    private fun selection(): WtsEvalIndividual<T> {

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