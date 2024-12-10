package org.evomaster.core.search.service.mutator

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class EmptyStructureMutator : StructureMutator() {


    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        //DO nothing
    }

    override fun addAndHarvestExternalServiceActions(
        individual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ): Boolean {
        //DO nothing
        return false
    }

    override fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        //DO nothing
    }

    override fun mutateInitStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        //DO nothing
    }
}