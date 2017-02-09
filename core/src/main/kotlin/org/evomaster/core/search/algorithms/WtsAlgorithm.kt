package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Mutator
import org.evomaster.core.search.service.SearchAlgorithm

/**
 * An implementation of the Whole Test Suite (WTS) algorithm,
 * as used in EvoSuite.
 * <br>
 * Each individual is a set of test cases (ie a test suite),
 * and we use a population algorithm as Genetic Algorithm (GA).
 * But still use an archive to do not lose best individuals.
 * <br>
 * Note: unless some unknown side-effect, or bug, WTS should be
 * definitively worse than MIO.
 * The implementation here is only experiments, not something to
 * use in EvoMaster
 */
class WtsAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var mutator: Mutator<T>

    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()


    override fun search(): Solution<T> {

        time.startSearch()
        population.clear()

        initPopulation()
        val n = config.populationSize

        while (time.shouldContinueSearch()) {

            //new generation

            val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

            while (nextPop.size < n) {

                val x = selection()
                val y = selection()
                //x and y are copied
                xover(x, y)
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

        return archive.extractSolution()
    }

    private fun mutate(wts: WtsEvalIndividual<T>){

        val op = randomness.choose(listOf("del","add","mod"))
        val n = wts.suite.size
        when(op){
            "del" -> if(n > 1){
                val i = randomness.nextInt(n)
                wts.suite.removeAt(i)
            }
            "add" -> if(n < config.maxSearchSuiteSize){
                val ind = ff.calculateCoverage(sampler.sampleAtRandom())
                archive.addIfNeeded(ind)
                wts.suite.add(ind)
            }
            "mod" -> {
                val i = randomness.nextInt(n)
                val ind = wts.suite[i]
                val nMutations = apc.getNumberOfMutations()

                mutator.mutateAndSave(nMutations, ind, archive)
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


    private fun xover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>) {

        val nx = x.suite.size
        val ny = y.suite.size

        val i = randomness.nextInt(Math.min(nx, ny))

        (0..i).forEach {
            val k = x.suite[i]
            x.suite[i] = y.suite[i]
            y.suite[i] = k
        }
    }

    private fun initPopulation() {

        val n = config.populationSize

        (1..n).forEach {
            population.add(sampleSuite())

            if (!time.shouldContinueSearch()) {
                return@forEach
            }
        }
    }

    private fun sampleSuite(): WtsEvalIndividual<T> {

        val n = 1 + randomness.nextInt(config.maxSearchSuiteSize)

        val wts = WtsEvalIndividual<T>(mutableListOf())

        (1..n).forEach {
            val ind = ff.calculateCoverage(sampler.sampleAtRandom())
            archive.addIfNeeded(ind)
            wts.suite.add(ind)

            if (!time.shouldContinueSearch()) {
                return@forEach
            }
        }

        return wts
    }
}