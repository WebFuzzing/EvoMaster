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

    companion object {
        val SUCCESS = AssertionRepairResult(success = true)
        val FAILURE = AssertionRepairResult(success = false)
    }
}