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
     * Maximum leading characters of [value] that can be absorbed across [genes]
     * left-to-right, without mutating anything.
     */
    fun absorbableCount(genes: List<Gene>, value: String): Int {
        if (value.isEmpty()) {
            return 0
        }
        var consumed = 0
        for (gene in genes) {
            if (consumed >= value.length) {
                break
            }
            val absorbable = gene as RxAbsorbable
            val canTake = absorbable.absorbableCount(value.substring(consumed))
            if (canTake > 0) {
                consumed += canTake
                continue
            }
            if (absorbable.canBeZeroWidth()) {
                continue
            }
            return 0
        }
        return consumed
    }

    /**
     * Forces as much of [value] as possible into [genes] left-to-right, mutating each
     * gene in place using each gene's [RxAbsorbable.tryForce]. Returns total characters placed.
     */
    fun tryForce(genes: List<Gene>, value: String): Int {
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
            val placed = absorbable.tryForce(remaining)
            if (placed == 0) {
                if (absorbable.canBeZeroWidth()) {
                    absorbable.forceZeroWidth()
                    continue
                } else {
                    return 0
                }
            }
            consumed += placed
        }
        return consumed
    }
}
