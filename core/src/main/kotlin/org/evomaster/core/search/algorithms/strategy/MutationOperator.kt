package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator

interface MutationOperator {
    fun <T : Individual> apply(
        wts: WtsEvalIndividual<T>,
        config: EMConfig,
        randomness: Randomness,
        mutator: Mutator<T>,
        ff: FitnessFunction<T>,
        sampler: Sampler<T>,
        archive: Archive<T>
    )
}


