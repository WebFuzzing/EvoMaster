package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual


class EmptyStructureMutator : StructureMutator() {


    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        //DO nothing
    }

    override fun mutateStructure(
        individual: Individual,
        evaluated: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ) {
        //DO nothing
    }
}