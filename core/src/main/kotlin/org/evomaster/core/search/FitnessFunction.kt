package org.evomaster.core.search


abstract class FitnessFunction<T>  where T : Individual {

    abstract fun calculateCoverage(individual: T) : EvaluatedIndividual<T>

//    abstract fun relati
}