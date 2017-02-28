package org.evomaster.core.search.mutator

import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.StructureMutator


class EmptyStructureMutator : StructureMutator() {

    override fun mutateStructure(individual: Individual) {
        //DO nothing
    }
}