package org.evomaster.core.search.gene.utils

import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.gene.regex.DisjunctionRxGene

/**
 * Result of [DisjunctionRxGene.attemptAssertionRepair] / [DisjunctionListRxGene.attemptAssertionRepair].
 *
 * Assertion repair for one scope (a [DisjunctionRxGene]'s own direct terms) can be successful,
 * fail or be conditionally successful, depending on either an external prefix/postfix (or both).
 *
 * [neededPrefix] is a requirement on whatever precedes this scope (from a lookbehind-direction);
 * [neededPostfix] is a requirement on whatever follows it (lookahead-direction). Each is
 * either `null` (no requirement in that direction), `""` (everything further out on that side must
 * also collapse to zero width), or a non-empty literal that must be absorbed there.
 */
data class AssertionRepairResult(
    val success: Boolean,
    val neededPrefix: String? = null,
    val neededPostfix: String? = null
) {
    init {
        require(success || (neededPrefix == null && neededPostfix == null)) {
            "A failed AssertionRepairResult cannot carry an outward requirement"
        }
    }

    /**
     * Combines this result with [next], the outcome of resolving one more requirement in the
     * same scope. Failure on either side wins outright, otherwise [next]'s own
     * [neededPrefix]/[neededPostfix] each take priority over this result's, falling back to
     * this result's own value when [next] did not set one of them.
     */
    fun mergedWith(next: AssertionRepairResult): AssertionRepairResult {
        if (!success || !next.success) {
            return FAILURE
        }
        return AssertionRepairResult(
            success = true,
            neededPrefix = next.neededPrefix ?: neededPrefix,
            neededPostfix = next.neededPostfix ?: neededPostfix
        )
    }

    companion object {
        val SUCCESS = AssertionRepairResult(success = true)
        val FAILURE = AssertionRepairResult(success = false)

        /**
         * A successful result whose only outcome is [remainder] still being needed by whatever
         * lies further out, depending on [backward].
         */
        fun stillNeeded(remainder: String, backward: Boolean): AssertionRepairResult =
            if (backward) {
                AssertionRepairResult(success = true, neededPrefix = remainder)
            } else {
                AssertionRepairResult(success = true, neededPostfix = remainder)
            }
    }
}