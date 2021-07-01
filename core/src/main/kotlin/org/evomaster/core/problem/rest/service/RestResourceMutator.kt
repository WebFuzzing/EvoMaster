package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StandardMutator

/**
 * resource-based mutator
 * i.e., the standard mutator handles resource-based rest individual
 */
class ResourceRestMutator : StandardMutator<RestIndividual>() {

    @Inject
    private lateinit var rm : ResourceManageService

    @Inject
    private lateinit var dm : ResourceDepManageService

    override fun postActionAfterMutation(mutatedIndividual: RestIndividual, mutated: MutatedGeneSpecification?) {
        // repair db among dbactions
        super.postActionAfterMutation(mutatedIndividual, null)
    }

    override fun doesStructureMutation(individual : RestIndividual): Boolean {

        return super.doesStructureMutation(individual)  &&
                (!dm.onlyIndependentResource())  // if all resources are asserted independent, there is no point to do structure mutation
                && dm.canMutateResource(individual)
    }


    override fun genesToMutation(individual: RestIndividual, evi: EvaluatedIndividual<RestIndividual>, targets: Set<Int>): List<Gene> {
        val restGenes = individual.getResourceCalls().filter(RestResourceCalls::isMutable).flatMap { it.seeGenes(
            GeneFilter.NO_SQL
        ) }.filter(Gene::isMutable)

        if (!config.generateSqlDataWithSearch)
            return restGenes

        // 1) SQL genes in initialization plus 2) SQL genes in resource handling plus 3) rest actions in resource handling
        return individual.seeInitializingActions().flatMap { it.seeGenes() }.filter(Gene::isMutable).plus(
            individual.getResourceCalls().filter(RestResourceCalls::isMutable).flatMap { it.seeGenes(GeneFilter.ONLY_SQL) }
        ).plus(restGenes)

    }

    override fun update(previous: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>, mutatedGenes: MutatedGeneSpecification?, mutationEvaluated: EvaluatedMutation) {
        /*
            update resource dependency after mutating structure of the resource-based individual
            NOTE THAT [this] can be only applied with MIO. In MIO, [mutatedGenes] must not be null.
         */
        if(mutatedGenes!!.mutatedGenes.isEmpty()
            && (previous.individual.getResourceCalls().size > 1 || mutated.individual.getResourceCalls().size > 1)
            && config.probOfEnablingResourceDependencyHeuristics > 0){

            dm.detectDependencyAfterStructureMutation(previous, mutated, mutationEvaluated)
        }

        /*
         TODO Man update resource dependency after do standard mutation?
         */
    }

}