package org.evomaster.core.problem.rest.service.mutator

import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.sql.SqlInsertBuilder

class ArazzoStructureMutator : ApiWsStructureMutator() {
    override fun mutateStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        TODO("Not yet implemented")
    }

    override fun addInitializingActions(
        individual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ) {
        TODO("Not yet implemented")
    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        TODO("Not yet implemented")
    }
}