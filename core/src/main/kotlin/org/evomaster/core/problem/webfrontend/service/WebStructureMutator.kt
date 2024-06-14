package org.evomaster.core.problem.webfrontend.service


import org.evomaster.core.Lazy
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.service.EnterpriseStructureMutator
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class WebStructureMutator: EnterpriseStructureMutator() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(WebStructureMutator::class.java)
    }

    @Inject
    private lateinit var sampler: WebSampler

    override fun canApplyActionStructureMutator(individual: Individual): Boolean {
        return true
    }

    override fun mutateStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        if (individual !is WebIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        //TODO here we could define several different strategies
        //we simply start with something very basic/naive

        mutateStructureAtRandom(individual, mutatedGenes)

        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    private fun mutateStructureAtRandom(ind: WebIndividual, mutatedGenes: MutatedGeneSpecification?) {
        val main = ind.seeMainActionComponents() as List<EnterpriseActionGroup<*>>

        if (main.size == 1) {
            val sampledAction = sampler.sampleUndefinedAction()

            ind.addMainActionInEmptyEnterpriseGroup(action=sampledAction)
            Lazy.assert { sampledAction.hasLocalId() }

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, ind.seeAllActions().size, localId = sampledAction.getLocalId(), false, ind.seeAllActions().size)

            return
        }

        if (randomness.nextBoolean() || main.size == config.maxTestSize) {

            //delete one at random
            log.trace("Deleting action from test")
            val chosen = randomness.choose(main.indices)

            //save mutated genes
            val removedActions = main[chosen].flatten()
            for(a in removedActions) {
                /*
                    FIXME: how does position work when adding/removing a subtree?
                 */
                mutatedGenes?.addRemovedOrAddedByAction(a, chosen, localId = main[chosen].getLocalId(),true, chosen)
            }
            ind.removeMainActionGroupAt(chosen)

        } else {

            //add one at random
            log.trace("Adding action to test")
            val sampledAction = sampler.sampleUndefinedAction()
            val chosen = randomness.choose(main.indices)
            ind.addMainActionInEmptyEnterpriseGroup(chosen, sampledAction)
            Lazy.assert { sampledAction.hasLocalId() }

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, chosen, localId = sampledAction.getLocalId(), false, chosen)
        }
    }

    override fun mutateInitStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        TODO("Not yet implemented")
    }

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
       //TODO name is confusing
    }

    override fun addAndHarvestExternalServiceActions(
        individual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ): Boolean {
       //TODO name is confusing
        return false
    }
}