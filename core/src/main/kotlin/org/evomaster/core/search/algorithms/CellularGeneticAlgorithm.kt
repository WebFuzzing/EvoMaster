package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * Cellular GA faithful to the standard pseudocode.
 * Neighborhood is mocked as a simple ring [left, self, right] for now.
 */
class CellularGeneticAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.CellularGA
    }

    override fun searchOnce() {
        beginGeneration()
        // Freeze targets for current generation
        frozenTargets = archive.notCoveredTargets()

        val n = population.size
        val next: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        for (i in 0 until n) {
            beginStep()
            val p = population[i]

            val neighbors = getNeighborhood(i)

            // Cellular tournament selection within neighborhood
            val x = neighborhoodTournament(neighbors)
            val y = neighborhoodTournament(neighbors)

            val o1 = x.copy()
            val o2 = y.copy()
            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(o1, o2)
            }

            val o: WtsEvalIndividual<T> = if (score(o1) >= score(o2)) o1 else o2

            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o)
            }

            val bestLocal: WtsEvalIndividual<T> = if (score(o) >= score(p)) o else p
            next.add(bestLocal)
            endStep()
        }

        population.clear()
        population.addAll(next)
        endGeneration()
    }

    /**
     * Runs tournament selection restricted to a neighborhood subset.
     */
    private fun neighborhoodTournament(neighbors: List<WtsEvalIndividual<T>>): WtsEvalIndividual<T> {
        val sel = selectionStrategy.select(neighbors, config.tournamentSize, randomness, ::score)
        observers.forEach { it.onSelection(sel) }
        return sel
    }

    /**
     * Returns the neighborhood list for a given index based on the configured model.
     * The returned list always includes the current cell ("center") and neighbors,
     * in the order customary for the chosen model.
     */
    private fun getNeighborhood(index: Int): List<WtsEvalIndividual<T>> {
        val model = config.cgaNeighborhoodModel
        val neighborhood = Neighborhood<T>(population.size)
        if (model == EMConfig.CGANeighborhoodModel.RING) {
            return neighborhood.ringTopology(population, index)
        }
        if (model == EMConfig.CGANeighborhoodModel.L5) {
            return neighborhood.linearFive(population, index)
        }
        if (model == EMConfig.CGANeighborhoodModel.C9) {
            return neighborhood.compactNine(population, index)
        }
        return neighborhood.compactThirteen(population, index)
    }
}


