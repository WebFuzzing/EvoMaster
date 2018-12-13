package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual


class EmptyStructureMutator : StructureMutator() {


    override fun addInitializingActions(individual: EvaluatedIndividual<*>) {
        //DO nothing
    }

    override fun mutateStructure(individual: Individual) {
        //DO nothing
    }
}