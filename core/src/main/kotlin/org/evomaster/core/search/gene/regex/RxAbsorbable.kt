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
 *
 * Regex genes that do not override the defaults are treated as non-absorbable (return 0,
 * throw on force attempt).
 */
interface RxAbsorbable {

    /**
     * Maximum number of leading characters of [value] this gene can be forced to
     * produce, or already produces without needing to change. Default 0 = cannot help.
     */
    fun absorbableCount(value: String): Int = 0

    /**
     * Places as many of [value]'s leading characters as possible, making the same
     * decision [absorbableCount] would for this same [value]. This mutates internal state in
     * place, and returns how many characters were actually placed.
     */
    fun tryForce(value: String): Int {
        throw IllegalStateException(
            "${this::class.simpleName} does not support forcing but tryForce was called"
        )
    }

    /**
     * Read-only: could tryForce("") succeed on this gene right now, without actually
     * calling it? Default false.
     */
    fun canBeZeroWidth(): Boolean = false
}