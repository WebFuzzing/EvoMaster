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
     */
    private fun walk(
        genes: List<Gene>,
        value: String,
        absorb: (RxAbsorbable, String) -> Int,
        onZeroWidth: (RxAbsorbable) -> Unit
    ): Int {
        if (value.isEmpty()) {
            return 0
        }
        var consumed = 0
        for (gene in genes) {
            if (consumed >= value.length) {
                break
            }
            val absorbable = gene as RxAbsorbable
            val remaining = value.substring(consumed)
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
        walk(genes, value,
            absorb = { gene, value -> gene.absorbableCount(value) },
            onZeroWidth = {}
        )

    /**
     * Forces as much of [value] as possible into [genes] left-to-right, mutating each
     * gene in place using each gene's [RxAbsorbable.tryForce]. Returns total characters placed.
     */
    fun tryForce(genes: List<Gene>, value: String): Int =
        walk(genes, value,
            absorb = { gene, value -> gene.tryForce(value) },
            onZeroWidth = { it.forceZeroWidth() }
        )
}