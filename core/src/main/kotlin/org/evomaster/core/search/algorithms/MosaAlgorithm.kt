package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy

/**
 * Implementation of MOSA from
 * "Automated Test Case Generation as a Many-Objective Optimisation Problem with Dynamic
 *  Selection of the Targets"
 */
class MosaAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    private var population: MutableList<EvaluatedIndividual<T>> = mutableListOf()

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.MOSA
    }

    override fun search(): Solution<T> {
        time.startSearch()
        population.clear()

        initPopulation()
        sortPopulation()

        val n = config.populationSize

        while (time.shouldContinueSearch()) {

            //new generation

            val nextPop: MutableList<EvaluatedIndividual<T>> = mutableListOf()

            while (nextPop.size < n) {

                var x = selection()

                x = getMutatator().mutateAndSave(1, x, archive)

                nextPop.add(x)

                if (!time.shouldContinueSearch()) {
                    break
                }
            }

            population.addAll(nextPop)
            sortPopulation()
            while (population.size > n) {
                population.removeAt(population.size - 1)
            }
        }

        return archive.extractSolution()
    }

    private class Data(val ind: EvaluatedIndividual<*>) {

        var rank = -1
        var crowdingDistance = -1
        var dominationByCounter = 0
        var dominatedSolutions: MutableList<Data> = mutableListOf()
    }


    private fun sortPopulation() {

        val notCovered = archive.notCoveredTargets()
        val list = population.map { ind -> Data(ind) }

        mosaPreferenceCriterion(notCovered, list)

        fastNonDominatedSort(notCovered, list)

        subvectorDominance(notCovered, list)

        population = list.sortedWith(compareBy<Data> { it.rank }.thenBy { - it.crowdingDistance })
                .map { d -> d.ind as EvaluatedIndividual<T>}
                .toMutableList()
    }

    private fun subvectorDominance(notCovered: Set<Int>, list: List<Data>){
        /*
            see:
            Substitute Distance Assignments in NSGA-II for
            Handling Many-Objective Optimization Problems
         */

        list.forEach { i ->
            i.crowdingDistance = 0
            list.filter { j -> j!=i }.forEach { j ->
                val v = svd(notCovered, i, j)
                if(v > i.crowdingDistance){
                    i.crowdingDistance = v
                }
            }
        }
    }


    private fun svd(notCovered: Set<Int>, i: Data, j: Data) : Int{
        var cnt = 0
        for(t in notCovered){
            if(i.ind.fitness.getHeuristic(t) > j.ind.fitness.getHeuristic(t)){
                cnt++
            }
        }
        return cnt
    }

    private fun fastNonDominatedSort(notCovered: Set<Int>, list: List<Data>) {
        /*
            see:
            A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II
         */

        var front: MutableList<Data> = mutableListOf()

        list.filter { d -> d.rank != 0 }.forEach { d ->
            //compare to each other
            list.filter { z -> z.rank != 0 }.forEach { z ->
                if (dominates(d, z, notCovered)) {
                    d.dominatedSolutions.add(z)
                } else if (dominates(z, d, notCovered)) {
                    d.dominationByCounter++
                }
            }
            if (d.dominationByCounter == 0) {
                d.rank = 1
                front.add(d)
            }
        }

        var i = 1
        while (!front.isEmpty()) {
            val Q: MutableList<Data> = mutableListOf()

            front.forEach { p ->
                p.dominatedSolutions.forEach { q ->
                    q.dominationByCounter--
                    if (q.dominationByCounter == 0) {
                        q.rank = i + 1
                        Q.add(q)
                    }
                }
            }
            i++
            front = Q
        }
    }

    private fun mosaPreferenceCriterion(notCovered: Set<Int>, list: List<Data>) {
        notCovered.forEach { t ->
            var chosen = list[0]
            list.forEach { data ->
                if (data.ind.fitness.getHeuristic(t) > chosen.ind.fitness.getHeuristic(t)) {
                    //recall: maximization problem
                    chosen = data
                }
            }
            //MOSA preference criterion: the best for a target gets Rank 0
            chosen.rank = 0
        }
    }

    private fun dominates(d: Data, z: Data, notCovered: Set<Int>): Boolean {
        return d.ind.fitness.subsumes(z.ind.fitness, notCovered)
    }

    private fun selection(): EvaluatedIndividual<T> {

        //assumed sorted population, where first are bests
        var min = population.size

        (0 until config.tournamentSize).forEach {
            val sel = randomness.nextInt(population.size)
            if (sel < min) {
                min = sel
            }
        }

        return population[min].copy()
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

    private fun sampleIndividual(): EvaluatedIndividual<T> {
        val ind = ff.calculateCoverage(sampler.sampleAtRandom())
        archive.addIfNeeded(ind)
        return ind
    }
}