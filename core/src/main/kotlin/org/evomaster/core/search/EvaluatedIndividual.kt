package org.evomaster.core.search


class EvaluatedIndividual<T>(val fitness: FitnessValue,
                             val individual: T,
                             val results: List<out ActionResult>)
where T : Individual {

    init{
        if(individual.seeActions().size != results.size){
            throw IllegalArgumentException("Actions and results size mismatch")
        }
    }


    fun copy(): EvaluatedIndividual<T> {
        return EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy)
                )
    }
}