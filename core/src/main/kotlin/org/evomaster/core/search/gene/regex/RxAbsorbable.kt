package org.evomaster.core.search.gene.regex

/**
 * Defines the interface for regex genes that can "absorb" part of a candidate
 * string during assertion repair.
 *
 * When a sampled string fails to satisfy an assertion, the repair loop samples a
 * candidate string from [AssertionRxGene.innerGene] and greedily walks the genes
 * around the assertion in its parent [DisjunctionRxGene]'s terms list,
 * asking each gene how much of the remaining candidate it can absorb and committing
 * that amount before moving to the next gene.
 */
interface RxAbsorbable {

    /**
     * Read-only: maximum number of leading characters of [value] this gene can be forced to
     * produce, or already produces without needing to change. Default 0 = cannot help.
     */
    fun absorbableCount(value: String): Int = 0

    /**
     * Places as many of [value]'s leading characters as possible and returns how many characters were actually placed.
     *
     * Precondition: [value] is not empty.
     */
    fun tryForce(value: String): Int {
        throw IllegalStateException(
            "${this::class.simpleName} does not support forcing but tryForce was called"
        )
    }

    /**
     * Read-only: could [forceZeroWidth] succeed on this gene (i.e.: "" can match this expression)?
     */
    val canBeZeroWidth: Boolean

    /**
     * Forces this gene to render as "". Unlike [tryForce], there is nothing to plan or report.
     *
     * Precondition: [canBeZeroWidth]
     */
    fun forceZeroWidth() {
        throw IllegalStateException(
            "${this::class.simpleName} cannot be zero-width but forceZeroWidth was called"
        )
    }

    /**
     * Suffix-anchored counterpart of [absorbableCount], used by lookbehind repair: how many of
     * [value]'s trailing characters could this gene be forced to produce.
     *
     * Default delegates to [absorbableCount] fed reversed [value]. This default is
     * used by genes that produce exactly one character, like [AnyCharacterRxGene].
     */
    fun absorbableSuffixCount(value: String): Int =
        if (value.isEmpty()) 0 else absorbableCount(value.reversed())

    /**
     * Suffix-anchored counterpart of [tryForce]: places as many of [value]'s trailing characters
     * as possible and returns how many were actually placed.
     *
     * Default delegates to [tryForce] fed reversed [value], in a similar way to [absorbableSuffixCount].
     *
     * Precondition: [value] is not empty.
     */
    fun tryForceSuffix(value: String): Int {
        require(value.isNotEmpty())
        val n = absorbableSuffixCount(value)
        if (n > 0) tryForce(value.reversed())
        return n
    }
}