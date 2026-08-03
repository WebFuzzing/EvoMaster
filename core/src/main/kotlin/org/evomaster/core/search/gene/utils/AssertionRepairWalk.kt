package org.evomaster.core.search.gene.utils

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RxAbsorbable

/**
 * Outcome of one greedy walk over a gene list: [consumed] is how much of the candidate string
 * can be placed. If less than the full string was consumed, [hardMismatch] says why. `true` means
 * a gene genuinely refused to match ([consumed] resets to 0 in that case) and the candidate is
 * simply wrong here; `false` means the gene list just ran out of room with no mismatch,
 * so the leftover is still worth trying to escape outwards.
 */

data class WalkOutcome(val consumed: Int, val hardMismatch: Boolean)

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
     * - [reversed]: whether to walk [genes] right-to-left for lookbehind or left-to-right for lookahead
     * @see WalkOutcome
     */
    private fun walk(
        genes: List<Gene>,
        value: String,
        absorb: (RxAbsorbable, String) -> Int,
        onZeroWidth: (RxAbsorbable) -> Unit,
        reversed: Boolean
    ): WalkOutcome {
        if (value.isEmpty()) {
            return WalkOutcome(0, hardMismatch = false)
        }
        var consumed = 0
        val walkTarget = if (reversed) {
            genes.asReversed()
        } else {
            genes
        }
        for (gene in walkTarget) {
            if (consumed >= value.length) {
                break
            }
            val absorbable = gene as RxAbsorbable
            val remaining = if (reversed) {
                value.dropLast(consumed)
            } else {
                value.drop(consumed)
            }
            val amount = absorb(absorbable, remaining)
            if (amount == 0) {
                if (absorbable.canBeZeroWidth) {
                    onZeroWidth(absorbable)
                    continue
                }
                return WalkOutcome(0, hardMismatch = true)
            }
            consumed += amount
        }
        return WalkOutcome(consumed, hardMismatch = false)
    }

    /**
     * Maximum leading characters of [value] that can be absorbed across [genes]
     * left-to-right, without mutating anything.
     * @see WalkOutcome
     */
    fun absorbableCount(genes: List<Gene>, value: String): WalkOutcome =
        walk(genes, value, reversed = false,
            absorb = { gene, value -> gene.absorbableCount(value) },
            onZeroWidth = {}
        )

    /**
     * Forces as much of [value] as possible into [genes] left-to-right, mutating each
     * gene in place using each gene's [RxAbsorbable.tryForce].
     * @see WalkOutcome
     */
    fun tryForce(genes: List<Gene>, value: String): WalkOutcome =
        walk(genes, value, reversed = false,
            absorb = { gene, value -> gene.tryForce(value) },
            onZeroWidth = { it.forceZeroWidth() }
        )

    /**
     * Suffix-anchored counterpart of [absorbableCount], used by lookbehind repair: maximum
     * trailing characters of [value] that can be absorbed across [genes] right-to-left
     * (walking [genes] in reverse, since the gene closest to the assertion's position is the
     * last one in [genes]), without mutating anything.
     * @see WalkOutcome
     */
    fun absorbableSuffixCount(genes: List<Gene>, value: String): WalkOutcome =
        walk(genes, value, reversed = true,
            absorb = { gene, value -> gene.absorbableSuffixCount(value) },
            onZeroWidth = {}
        )

    /**
     * Suffix-anchored counterpart of [tryForce], used by lookbehind repair: forces as much
     * of [value] as possible into [genes] right-to-left (mirroring [tryForce]), mutating each
     * gene in place using [RxAbsorbable.tryForceSuffix].
     * @see WalkOutcome
     */
    fun tryForceSuffix(genes: List<Gene>, value: String): WalkOutcome =
        walk(genes, value, reversed = true,
            absorb = { gene, value -> gene.tryForceSuffix(value) },
            onZeroWidth = { it.forceZeroWidth() }
        )
}