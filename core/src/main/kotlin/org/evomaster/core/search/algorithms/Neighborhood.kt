package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import kotlin.math.sqrt

/**
 * Neighborhood utilities inspired by EvoSuite's Neighbourhood for Cellular GA.
 * Reference: https://github.com/EvoSuite/evosuite/blob/master/client/src/main/java/org/evosuite/ga/Neighbourhood.java
 */
class Neighborhood<T : Individual>(private val populationSize: Int) : NeighborModels<WtsEvalIndividual<T>> {

    init {
        require(populationSize > 0) { "populationSize must be > 0" }
    }

    private fun validate(population: List<WtsEvalIndividual<T>>, index: Int) {
        require(population.size == populationSize) {
            "population size (" + population.size + ") must equal initialized populationSize (" + populationSize + ")"
        }
        require(index in 0 until populationSize) {
            "index " + index + " out of bounds for population of size " + populationSize
        }
    }

    /**
     * Map grid coordinate (rowIndex, colIndex) to a valid 1D index in [0, populationSize),
     * using wrap-around on both axes.
     */
    private fun toWrappedLinearIndex(numCols: Int, rowIndex: Int, colIndex: Int): Int {
        val numCells = populationSize
        val numRows = (numCells + numCols - 1) / numCols
        val wrappedRow = (rowIndex + numRows) % numRows
        val wrappedCol = (colIndex + numCols) % numCols
        val linearIndex = wrappedRow * numCols + wrappedCol
        if (linearIndex < numCells) {
            return linearIndex
        }
        return linearIndex % numCells
    }

    override fun ringTopology(population: List<WtsEvalIndividual<T>>, index: Int): List<WtsEvalIndividual<T>> {
        validate(population, index)
        val n = populationSize
        val left = population[(index - 1 + n) % n]
        val self = population[index]
        val right = population[(index + 1) % n]
        return listOf(left, self, right)
    }

    override fun linearFive(population: List<WtsEvalIndividual<T>>, index: Int): List<WtsEvalIndividual<T>> {
        validate(population, index)
        val n = populationSize
        val cols = maxOf(1, sqrt(n.toDouble()).toInt())
        val row = index / cols
        val col = index % cols

        val north = population[toWrappedLinearIndex(cols, row - 1, col)]
        val south = population[toWrappedLinearIndex(cols, row + 1, col)]
        val east = population[toWrappedLinearIndex(cols, row, col + 1)]
        val west = population[toWrappedLinearIndex(cols, row, col - 1)]
        val self = population[index]
        return listOf(north, south, east, west, self)
    }

    override fun compactNine(population: List<WtsEvalIndividual<T>>, index: Int): List<WtsEvalIndividual<T>> {
        validate(population, index)
        val n = populationSize
        val cols = maxOf(1, sqrt(n.toDouble()).toInt())
        val row = index / cols
        val col = index % cols

        val n1 = population[toWrappedLinearIndex(cols, row - 1, col)]
        val s1 = population[toWrappedLinearIndex(cols, row + 1, col)]
        val e1 = population[toWrappedLinearIndex(cols, row, col + 1)]
        val w1 = population[toWrappedLinearIndex(cols, row, col - 1)]
        val nw = population[toWrappedLinearIndex(cols, row - 1, col - 1)]
        val sw = population[toWrappedLinearIndex(cols, row + 1, col - 1)]
        val ne = population[toWrappedLinearIndex(cols, row - 1, col + 1)]
        val se = population[toWrappedLinearIndex(cols, row + 1, col + 1)]
        val self = population[index]
        return listOf(n1, s1, e1, w1, nw, sw, ne, se, self)
    }

    override fun compactThirteen(population: List<WtsEvalIndividual<T>>, index: Int): List<WtsEvalIndividual<T>> {
        validate(population, index)
        val n = populationSize
        val cols = maxOf(1, sqrt(n.toDouble()).toInt())
        val row = index / cols
        val col = index % cols

        val n1 = population[toWrappedLinearIndex(cols, row - 1, col)]
        val s1 = population[toWrappedLinearIndex(cols, row + 1, col)]
        val e1 = population[toWrappedLinearIndex(cols, row, col + 1)]
        val w1 = population[toWrappedLinearIndex(cols, row, col - 1)]
        val nw = population[toWrappedLinearIndex(cols, row - 1, col - 1)]
        val sw = population[toWrappedLinearIndex(cols, row + 1, col - 1)]
        val ne = population[toWrappedLinearIndex(cols, row - 1, col + 1)]
        val se = population[toWrappedLinearIndex(cols, row + 1, col + 1)]
        val nn = population[toWrappedLinearIndex(cols, row - 2, col)]
        val ss = population[toWrappedLinearIndex(cols, row + 2, col)]
        val ee = population[toWrappedLinearIndex(cols, row, col + 2)]
        val ww = population[toWrappedLinearIndex(cols, row, col - 2)]
        val self = population[index]
        return listOf(n1, s1, e1, w1, nw, sw, ne, se, nn, ss, ee, ww, self)
    }
}


