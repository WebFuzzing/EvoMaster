package org.evomaster.core.search.algorithms.strategy.suite

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator

/**
 * Default mutation operator acting at the test suite level for GA.
 *
 * Behavior:
 * - Randomly applies one of: "del", "add", "mod" (selected randomly)
 * - del: if size > 1, remove a random test from the suite.
 * - add: if size < maxSearchSuiteSize, sample + evaluate; add new test to suite (and archive if needed).
 * - mod: mutate one random test in the suite; re-evaluate (or mutateAndSave if GASolutionSource = ARCHIVE).
 */
class DefaultMutationEvaluationOperator : MutationEvaluationOperator {
    companion object {
        private const val OP_DELETE = "del"
        private const val OP_ADD = "add"
        private const val OP_MOD = "mod"
    }

    override fun <T : Individual> mutateEvaluateAndArchive(
        wts: WtsEvalIndividual<T>,
        config: EMConfig,
        randomness: Randomness,
        mutator: Mutator<T>,
        ff: FitnessFunction<T>,
        sampler: Sampler<T>,
        archive: Archive<T>
    ) {
        val op = randomness.choose(listOf(OP_DELETE, OP_ADD, OP_MOD))
        val n = wts.suite.size

        when (op) {
            OP_DELETE -> if (n > 1) {
                val i = randomness.nextInt(n)
                wts.suite.removeAt(i)
            }

            OP_ADD -> if (n < config.maxSearchSuiteSize) {
                ff.calculateCoverage(sampler.sample(), modifiedSpec = null)?.run {
                    if (config.gaSolutionSource == EMConfig.GASolutionSource.ARCHIVE) {
                        archive.addIfNeeded(this)
                    }
                    wts.suite.add(this)
                }
            }

            OP_MOD -> {
                val i = randomness.nextInt(n)
                val ind = wts.suite[i]

                if (config.gaSolutionSource == EMConfig.GASolutionSource.ARCHIVE) {
                    mutator.mutateAndSave(ind, archive)?.let { wts.suite[i] = it }
                } else {
                    val mutated = mutator.mutate(ind)
                    ff.calculateCoverage(mutated, modifiedSpec = null)?.let { wts.suite[i] = it }
                }
            }
        }
    }
}


