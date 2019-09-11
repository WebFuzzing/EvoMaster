package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Archive
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

    @Inject
    private lateinit var archive: Archive<RestIndividual>


    override fun postActionAfterMutation(mutatedIndividual: RestIndividual) {
        super.postActionAfterMutation(mutatedIndividual)
        mutatedIndividual.getResourceCalls().forEach { rm.repairRestResourceCalls(it) }
        mutatedIndividual.repairDBActions(rm.getSqlBuilder())
    }

    override fun doesStructureMutation(individual : RestIndividual): Boolean {

        return individual.canMutateStructure() &&
                (!dm.onlyIndependentResource()) && // if all resources are asserted independent, there is no point to do structure mutation
                config.maxTestSize > 1 &&
                randomness.nextBoolean(config.structureMutationProbability)
    }

    /**
     * TODO : support with SQL-related strategy
     */
    override fun genesToMutation(individual: RestIndividual, evi : EvaluatedIndividual<RestIndividual>): List<Gene> {
        //if data of resource call is existing from db, select other row
        val selectAction = individual.getResourceCalls().filter { it.dbActions.isNotEmpty() && it.dbActions.last().representExistingData }
        if(selectAction.isNotEmpty())
            return randomness.choose(selectAction).seeGenes()
        return individual.getResourceCalls().flatMap { it.seeGenes() }.filter(Gene::isMutable)
    }

    override fun update(previous: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>, mutatedGenes : MutatedGeneSpecification?) {
        /*
            update resource dependency after mutating structure of the resource-based individual
            NOTE THAT [this] can be only applied with MIO. In MIO, [mutatedGenes] must not be null.
         */
        if(mutatedGenes!!.mutatedGenes.isEmpty() && (previous.individual.getResourceCalls().size > 1 || mutated.individual.getResourceCalls().size > 1) && config.probOfEnablingResourceDependencyHeuristics > 0){
            val isWorse = previous.fitness.subsumes(
                    mutated.fitness,
                    archive.notCoveredTargets(),
                    config.secondaryObjectiveStrategy,
                    config.bloatControlForSecondaryObjective)
            val isBetter = archive.wouldReachNewTarget(mutated) || !isWorse
            dm.detectDependencyAfterStructureMutation(previous, mutated, if(isBetter) 1 else if(isWorse) -1 else 0)
        }

        /*
         TODO Man update resource dependency after do standard mutation?
         */
    }

}