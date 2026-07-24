package org.evomaster.core.search.gene.utils

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RxAbsorbable

/**
 * Greedy-walk utilities for assertion repair.
 *
 * Given a candidate string sampled from [org.evomaster.core.search.gene.regex.AssertionRxGene.innerGene], attempts to place
 * as much of it as possible into a sequence of genes by asking each gene (via [org.evomaster.core.search.gene.regex.RxAbsorbable])
 * how much it can take and committing that amount.
 */
object AssertionRepairWalk {
    /**
     * Shared algorithm behind all public functions below, these differ only in:
     * - [absorb]: which [RxAbsorbable] operation to call per gene (a read-only count, or a mutating force)
     * - [onZeroWidth]: what to do when [absorb] returns 0 and the gene [RxAbsorbable.canBeZeroWidth]
     * - [forward]: whether to walk [genes] left-to-right for lookahead or right-to-left for lookbehind
     */
    private fun walk(
        genes: List<Gene>,
        value: String,
        absorb: (RxAbsorbable, String) -> Int,
        onZeroWidth: (RxAbsorbable) -> Unit,
        forward: Boolean
    ): Int {
        if (value.isEmpty()) {
            return 0
        }
        var consumed = 0
        val walkTarget = if (forward) {
            genes
        } else {
            genes.asReversed()
        }
        for (gene in walkTarget) {
            if (consumed >= value.length) {
                break
            }
            val absorbable = gene as RxAbsorbable
            val remaining = if (forward) {
                value.substring(consumed)
            } else {
                value.dropLast(consumed)
            }
            val amount = absorb(absorbable, remaining)
            if (amount == 0) {
                if (absorbable.canBeZeroWidth) {
                    onZeroWidth(absorbable)
                    continue
                }
                return 0
            }
            consumed += amount
        }
        return consumed
    }

    /**
     * Maximum leading characters of [value] that can be absorbed across [genes]
     * left-to-right, without mutating anything.
     */
    fun absorbableCount(genes: List<Gene>, value: String): Int =
        walk(genes, value, forward = true,
            absorb = { gene, value -> gene.absorbableCount(value) },
            onZeroWidth = {}
        )

    /**
     * Forces as much of [value] as possible into [genes] left-to-right, mutating each
     * gene in place using each gene's [RxAbsorbable.tryForce]. Returns total characters placed.
     */
    fun tryForce(genes: List<Gene>, value: String): Int =
        walk(genes, value, forward = true,
            absorb = { gene, value -> gene.tryForce(value) },
            onZeroWidth = { it.forceZeroWidth() }
        )

    /**
     * Suffix-anchored counterpart of [absorbableCount], used by lookbehind repair: maximum
     * trailing characters of [value] that can be absorbed across [genes] right-to-left
     * (walking [genes] in reverse, since the gene closest to the assertion's position is the
     * last one in [genes]), without mutating anything.
     */
    fun absorbableSuffixCount(genes: List<Gene>, value: String): Int =
        walk(genes, value, forward = false,
            absorb = { gene, value -> gene.absorbableSuffixCount(value) },
            onZeroWidth = {}
        )

    /**
     * Suffix-anchored counterpart of [tryForce], used by lookbehind repair: forces as much
     * of [value] as possible into [genes] right-to-left (mirroring [tryForce]), mutating each
     * gene in place using [RxAbsorbable.tryForceSuffix]. Returns total characters placed.
     */
    fun tryForceSuffix(genes: List<Gene>, value: String): Int =
        walk(genes, value, forward = false,
            absorb = { gene, value -> gene.tryForceSuffix(value) },
            onZeroWidth = { it.forceZeroWidth() }
        )
}