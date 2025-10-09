package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator

/**
 * Spy mutation operator counting how many times mutation is applied and
 * recording which individuals were mutated, while delegating to the default
 * implementation for realistic behavior.
 */
class SpyMutationOperator : MutationOperator {
    private var callCount: Int = 0
    val mutated = mutableListOf<WtsEvalIndividual<*>>()

    fun getCallCount(): Int = callCount

    override fun <T : Individual> apply(
        wts: WtsEvalIndividual<T>,
        config: EMConfig,
        randomness: Randomness,
        mutator: Mutator<T>,
        ff: FitnessFunction<T>,
        sampler: Sampler<T>,
        archive: Archive<T>
    ) {
        callCount++
        mutated.add(wts)
        DefaultMutationOperator().apply(wts, config, randomness, mutator, ff, sampler, archive)
    }
}


