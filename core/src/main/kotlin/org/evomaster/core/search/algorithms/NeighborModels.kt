package org.evomaster.core.search.algorithms

/**
 * An interface that defines the four neighbourhood models used with the cGA
 */
interface NeighborModels<T> {

    fun ringTopology(collection: List<T>, position: Int): List<T>

    fun linearFive(collection: List<T>, position: Int): List<T>

    fun compactNine(collection: List<T>, position: Int): List<T>

    fun compactThirteen(collection: List<T>, position: Int): List<T>
}




