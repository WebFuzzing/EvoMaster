package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator

class DefaultMutationOperator : MutationOperator {
    override fun <T : Individual> apply(
        wts: WtsEvalIndividual<T>,
        config: EMConfig,
        randomness: Randomness,
        mutator: Mutator<T>,
        ff: FitnessFunction<T>,
        sampler: Sampler<T>,
        archive: Archive<T>
    ) {
        val op = randomness.choose(listOf("del", "add", "mod"))
        val n = wts.suite.size

        when (op) {
            "del" -> if (n > 1) {
                val i = randomness.nextInt(n)
                wts.suite.removeAt(i)
            }

            "add" -> if (n < config.maxSearchSuiteSize) {
                ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
                    if (config.gaSolutionSource != EMConfig.GASolutionSource.POPULATION) {
                        archive.addIfNeeded(this)
                    }
                    wts.suite.add(this)
                }
            }

            "mod" -> {
                val i = randomness.nextInt(n)
                val ind = wts.suite[i]

                if (config.gaSolutionSource == EMConfig.GASolutionSource.POPULATION) {
                    val mutated = mutator.mutate(ind)
                    ff.calculateCoverage(mutated, modifiedSpec = null)?.let { wts.suite[i] = it }
                } else {
                    mutator.mutateAndSave(ind, archive)?.let { wts.suite[i] = it }
                }
            }
        }
    }
}


