package org.evomaster.core.problem.rest2

import com.google.inject.Inject
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.mutator.StandardMutator

class RestResourceMutator : StandardMutator<RestIndividualII>() {
    @Inject
    private lateinit var rm :ResourceManageService

    override fun repairAfterMutation(individual: RestIndividualII) {
        super.repairAfterMutation(individual)
        individual.getResourceCalls().forEach(RestResourceCalls::repairGenesAfterMutation)
        individual.getResourceCalls().forEach { rm.repairRestResourceCalls(it) }
    }

    override fun doesStructureMutation(individual : RestIndividualII): Boolean {

        return individual.canMutateStructure() &&
                (!rm.onlyIndependentResource()) && // if all resources are asserted independent, there is no point to do structure mutation
                config.maxTestSize > 1 &&
                randomness.nextBoolean(config.structureMutationProbability)
    }

    override fun genesToMutation(individual: RestIndividualII, evi : EvaluatedIndividual<RestIndividualII>): List<Gene> {
        //if data of resource call is existing from db, select other row
        val selectAction = individual.getResourceCalls().filter { it.dbActions.isNotEmpty() && it.dbActions.last().representExistingData }
        if(selectAction.isNotEmpty())
            return randomness.choose(selectAction).seeGenes()
        return individual.getResourceCalls().flatMap { it.seeGenes() }.filter(Gene::isMutable)
    }



}