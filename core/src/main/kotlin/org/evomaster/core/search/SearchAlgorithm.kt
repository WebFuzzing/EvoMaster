package org.evomaster.core.search


abstract class SearchAlgorithm<T> where T : Individual{


    abstract fun search(iterations: Int) : Solution<T>

}
