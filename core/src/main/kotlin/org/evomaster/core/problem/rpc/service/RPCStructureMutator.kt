package org.evomaster.core.problem.rpc.service

import com.google.inject.Inject
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification

/**
 * created by manzhang on 2021/11/26
 */
class RPCStructureMutator : ApiWsStructureMutator() {

    @Inject
    private lateinit var sampler: RPCSampler

    override fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        if (individual !is RPCIndividual) {
            throw IllegalArgumentException("Invalid: individual type to be mutated with RPCStructureMutator should be RPCIndividual but ${individual::class.java.simpleName}")
        }

        if (!individual.canMutateStructure()) return
        if (config.maxTestSize == 1) return

        mutateForRandomType(individual, mutatedGenes)
        /*
            TODO Man other smart strategies
         */
        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    private fun mutateForRandomType(individual: RPCIndividual, mutatedGenes: MutatedGeneSpecification?) {

        val size = individual.seeMainExecutableActions().size
        if ((size + 1 < config.maxTestSize) && (size == 1 || randomness.nextBoolean())){
            // add
            val sampledAction = sampler.sampleRandomAction()

            //save mutated genes
            individual.addAction(action = sampledAction)
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, individual.seeFixedMainActions().indexOf(sampledAction), null, false, size)

        }else{
            // remove
            val chosen = randomness.choose(individual.seeMainActionComponents().indices)
            val removed = individual.seeMainExecutableActions()[chosen]
            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(removed, individual.seeFixedMainActions().indexOf(removed), null, true, size)
            individual.removeAction(chosen)
        }
    }

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        addInitializingActions(individual, mutatedGenes, sampler)
    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        return sampler.sqlInsertBuilder
    }
}