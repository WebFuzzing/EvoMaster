package org.evomaster.core.search

/**
As the number of targets is unknown, we cannot have
a minimization problem, as new targets could be added
throughout the search
 */
abstract class FitnessFunction<T>  where T : Individual {

    abstract fun calculateCoverage(individual: T) : Double

//    abstract fun relati
}