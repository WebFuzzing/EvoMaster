package org.evomaster.core.problem.asyncapi.service.structure

import com.google.inject.Inject
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.asyncapi.service.sampler.AsyncAPISampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * Adds or removes pairs of AsyncAPI actions while preserving the
 * PUBLISH/SUBSCRIBE_REPLY adjacency invariant: any SUBSCRIBE_REPLY action is
 * removed alongside its paired PUBLISH, and a freshly-sampled action group
 * is inserted as a unit.
 */
class AsyncAPIStructureMutator : StructureMutator() {

    @Inject
    private lateinit var sampler: AsyncAPISampler

    override fun mutateStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        if (individual !is AsyncAPIIndividual) return

        val actions = individual.seeMainExecutableActions()
        if (actions.isEmpty()) {
            // Always seed with at least one paired group when the individual is empty.
            appendRandomGroup(individual, mutatedGenes)
            return
        }

        // A group is a PUBLISH with its trailing SUBSCRIBE_REPLY siblings (1
        // or 2 actions).  Removal operates on whole groups, so we must keep
        // at least one group so the individual isn't emptied.
        val groupCount = actions.distinctBy { it.pairId }.size
        val canAdd = actions.size + 2 <= config.maxTestSize
        val canRemove = groupCount > 1
        val add = when {
            canAdd && !canRemove -> true
            !canAdd && canRemove -> false
            !canAdd && !canRemove -> return
            else -> randomness.nextBoolean()
        }

        if (add) {
            appendRandomGroup(individual, mutatedGenes)
        } else {
            removeRandomGroup(individual, mutatedGenes)
        }
    }

    override fun mutateInitStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        // No initialization actions for AsyncAPI in the starter slice.
    }

    override fun addInitializingActions(
        individual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ) {
        // No-op.
    }

    override fun addAndHarvestExternalServiceActions(
        individual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?
    ): Boolean {
        return false
    }

    private fun appendRandomGroup(
        individual: AsyncAPIIndividual,
        mutatedGenes: MutatedGeneSpecification?
    ) {
        val fresh = sampler.sample(forceRandomSample = true).seeMainExecutableActions()
        if (fresh.isEmpty()) return
        // Sample-at-random already produces 1+ actions; we take the first
        // PUBLISH and (if present) its trailing SUBSCRIBE_REPLY twin so
        // adjacency is preserved.
        val first = fresh.first()
        val followers = mutableListOf<AsyncAPIAction>()
        for (i in 1 until fresh.size) {
            if (fresh[i].pairId == first.pairId) followers.add(fresh[i]) else break
        }
        val sizeBefore = individual.seeMainExecutableActions().size
        individual.addAction(first)
        mutatedGenes?.addRemovedOrAddedByAction(
            first,
            individual.seeFixedMainActions().indexOf(first),
            null,
            removed = false,
            resourcePosition = sizeBefore
        )
        followers.forEachIndexed { offset, follower ->
            individual.addAction(follower)
            mutatedGenes?.addRemovedOrAddedByAction(
                follower,
                individual.seeFixedMainActions().indexOf(follower),
                null,
                removed = false,
                resourcePosition = sizeBefore + 1 + offset
            )
        }
    }

    private fun removeRandomGroup(
        individual: AsyncAPIIndividual,
        mutatedGenes: MutatedGeneSpecification?
    ) {
        val actions = individual.seeMainExecutableActions()
        // Pick a PUBLISH at random; remove it and any adjacent SUBSCRIBE_REPLY
        // sharing its pairId.
        val publishIndices = actions.withIndex()
            .filter { (_, a) -> a.kind == AsyncAPIAction.Kind.PUBLISH }
            .map { it.index }
        if (publishIndices.isEmpty()) return
        val target = randomness.choose(publishIndices)
        val pairId = actions[target].pairId
        // Collect followers (adjacent actions sharing pairId) before removal.
        val followerIndices = mutableListOf<Int>()
        var j = target + 1
        while (j < actions.size && actions[j].pairId == pairId) {
            followerIndices.add(j)
            j++
        }
        // Record removals BEFORE physically removing, so indexOf still works.
        followerIndices.reversed().forEach { idx ->
            val removed = actions[idx]
            mutatedGenes?.addRemovedOrAddedByAction(
                removed,
                individual.seeFixedMainActions().indexOf(removed),
                null,
                removed = true,
                resourcePosition = idx
            )
            individual.removeAction(idx)
        }
        val removedHead = actions[target]
        mutatedGenes?.addRemovedOrAddedByAction(
            removedHead,
            individual.seeFixedMainActions().indexOf(removedHead),
            null,
            removed = true,
            resourcePosition = target
        )
        individual.removeAction(target)
    }
}
