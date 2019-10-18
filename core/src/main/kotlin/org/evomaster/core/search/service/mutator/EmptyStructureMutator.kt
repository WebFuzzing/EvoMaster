package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class EmptyStructureMutator : StructureMutator() {


    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        //DO nothing
    }

    override fun mutateStructure(individual: Individual, mutatedGenes: MutatedGeneSpecification?) {
        //DO nothing
    }
}