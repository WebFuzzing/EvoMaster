package org.evomaster.core.search


class EvaluatedIndividual<T>(val fitness: FitnessValue, val individual: T)
    where T : Individual {


    fun copy() : EvaluatedIndividual<T> {
        return EvaluatedIndividual(fitness.copy(), individual.copy() as T)
    }


    //TODO equals for Map

    //TODO add execution result
}