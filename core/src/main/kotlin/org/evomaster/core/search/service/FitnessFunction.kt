package org.evomaster.core.search.service

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual


abstract class FitnessFunction<T>  where T : Individual {

    abstract fun calculateCoverage(individual: T) : EvaluatedIndividual<T>

//    abstract fun relati
}