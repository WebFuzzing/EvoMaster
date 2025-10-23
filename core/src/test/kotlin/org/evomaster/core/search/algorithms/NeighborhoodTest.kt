package org.evomaster.core.search.algorithms

import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.FitnessValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeighborhoodTest {

    private fun populationOf(size: Int): List<WtsEvalIndividual<OneMaxIndividual>> {
        val pop = mutableListOf<WtsEvalIndividual<OneMaxIndividual>>()
        repeat(size) {
            pop.add(WtsEvalIndividual(mutableListOf()))
        }
        return pop
    }

    @Test
    fun ringTopology_left_and_right() {
        val pop = populationOf(16)
        val n = Neighborhood<OneMaxIndividual>(pop.size)
        val neighbors = n.ringTopology(pop, 2)
        assertEquals(pop[1], neighbors[0]) // left
        assertEquals(pop[3], neighbors[2]) // right
    }

    @Test
    fun ringTopology_wrap_edges() {
        val pop = populationOf(16)
        val n = Neighborhood<OneMaxIndividual>(pop.size)
        val leftEdge = n.ringTopology(pop, 0)
        assertEquals(pop[15], leftEdge[0])
        val rightEdge = n.ringTopology(pop, 15)
        assertEquals(pop[0], rightEdge[2])
    }

    @Test
    fun linearFive_cardinals() {
        val pop = populationOf(16)
        val n = Neighborhood<OneMaxIndividual>(pop.size)
        val neighbors = n.linearFive(pop, 5)
        assertEquals(pop[1], neighbors[0]) // N
        assertEquals(pop[9], neighbors[1]) // S
        assertEquals(pop[6], neighbors[2]) // E
        assertEquals(pop[4], neighbors[3]) // W
    }

    @Test
    fun compactNine_diagonals() {
        val pop = populationOf(16)
        val n = Neighborhood<OneMaxIndividual>(pop.size)
        val neighbors = n.compactNine(pop, 5)
        assertEquals(pop[2], neighbors[6]) // NE
        assertEquals(pop[0], neighbors[4]) // NW
        assertEquals(pop[10], neighbors[7]) // SE
        assertEquals(pop[8], neighbors[5]) // SW
    }

    @Test
    fun compactThirteen_secondRing() {
        val pop = populationOf(16)
        val n = Neighborhood<OneMaxIndividual>(pop.size)
        val neighbors = n.compactThirteen(pop, 10)
        assertEquals(pop[2], neighbors[8])  // NN
        assertEquals(pop[2], neighbors[9])  // SS (based on the Java test mapping)
    }
}


