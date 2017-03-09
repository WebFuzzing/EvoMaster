package org.evomaster.experiments.carfast

import com.google.inject.Inject
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.StructureMutator


class ISFStructureMutator : StructureMutator() {

    @Inject
    private lateinit var sampler: ISFSampler


    override fun mutateStructure(individual: Individual) {
        if (individual !is ISFIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        if (!individual.canMutateStructure()) {
            throw IllegalStateException()
        }

        val action = sampler.sampleAction()

        individual.action = action
    }

}