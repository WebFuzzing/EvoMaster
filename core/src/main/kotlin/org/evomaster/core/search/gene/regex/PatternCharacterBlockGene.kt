package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.utils.RegexFlags

/**
 * This class is immutable unless the regular expression has the CASE_INSENSITIVE flag on and at least one caseable
 * character, which may depend on the state of UNICODE_CASE flag as well. If the flag CASE_INSENSITIVE is on and there
 * is at least one caseable character, the class only allows case mutations for the caseable characters present.
 * @see org.evomaster.core.utils.RegexFlags.isCaseable
 */
class PatternCharacterBlockGene(
    name: String,
    val stringBlock: String,
    val flags: RegexFlags = RegexFlags()
) : RxAtom, SimpleGene(name) {

    /**
     * Per-character case choice: true = uppercase, false = lowercase.
     * Only meaningful when flags.caseInsensitive is true.
     */
    var caseChoices: BooleanArray = BooleanArray(stringBlock.length) { false }

    override fun isMutable(): Boolean {
        // check if there are any caseable characters (this also checks the CASE_INSENSITIVE flag value)
        return stringBlock.any { c -> flags.isCaseable(c) }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        val copy = PatternCharacterBlockGene(name, stringBlock, flags)
        copy.caseChoices = caseChoices.copyOf() //copy the current casings too
        return copy
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ${this.javaClass.simpleName}")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        /* This class is mutable only if the CASE_INSENSITIVE flag is on and at least one caseable character is present.
         If this is the case we can randomize the casings for those characters. */
        if (!isMutable()) {
            throw IllegalStateException("Not supposed to mutate immutable ${this.javaClass.simpleName}")
        }

        val previous = caseChoices.copyOf()

        caseChoices = BooleanArray(stringBlock.length) { i ->
            if (flags.isCaseable(stringBlock[i])) {
                randomness.nextBoolean()
            }
            else {
                false
            }
        }

        if(tryToForceNewValue && caseChoices.contentEquals(previous)){
            randomize(randomness, tryToForceNewValue)
        }
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        /* This class is mutable only if the CASE_INSENSITIVE flag is on and at least one caseable character is present.
         If this is the case we can mutate by randomly flipping the casing for one of those characters. */
        if (!isMutable()) {
            throw IllegalStateException("Not supposed to mutate immutable ${this.javaClass.simpleName}")
        }
        val caseableIndices = stringBlock.indices.filter { i ->
            flags.isCaseable(stringBlock[i])
        }
        val i = randomness.choose(caseableIndices)
        caseChoices[i] = !caseChoices[i]
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
    // We apply the case selected for each character (for the caseable characters)
        if (!isMutable()) {
            return stringBlock
        }
        return stringBlock.mapIndexed { i, c ->
            when {
                !flags.isCaseable(c) -> c
                caseChoices[i] -> c.uppercaseChar()
                else -> c.lowercaseChar()
            }
        }.joinToString("")
    }



    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is PatternCharacterBlockGene) {
            return false
        }
        // we need to consider casings too, therefore we can just compare the strings using getValueAsPrintableString
        return getValueAsPrintableString(targetFormat = null) ==
                other.getValueAsPrintableString(targetFormat = null)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        //do nothing
        return containsSameValueAs(other)
    }

    /**
     * Maps a forward walk-position [index] to the real index into [value], depending on [reversed].
     */
    private fun realIndex(reversed: Boolean, value: String, index: Int): Int =
        if (reversed) {
            value.lastIndex - index
        } else {
            index
        }

    /**
     * Shared body behind [absorbableCount]/[absorbableSuffixCount], they differ only in which
     * end of [stringBlock] and [value] the walk anchors to, via [reversed]. No partial matches,
     * returns 0 or the shortest string's length (which was matched).
     */
    private fun matchCount(value: String, reversed: Boolean): Int {
        var i = 0
        while (i < value.length && i < stringBlock.length) {
            val blockIdx = realIndex(reversed, stringBlock, i)
            val valueIdx = realIndex(reversed, value, i)
            val c = stringBlock[blockIdx]
            val matches = if (flags.isCaseable(c)) {
                value[valueIdx].equals(c, ignoreCase = true)
            } else {
                value[valueIdx] == c
            }
            if (!matches) {
                return 0
            }
            i++
        }
        return i
    }

    /**
     * Shared body behind [tryForce]/[tryForceSuffix]: commits the case (upper/lower) of the
     * first (or last if [reversed]) [matchedLength] characters of [stringBlock] to match
     * [value]'s corresponding characters.
     */
    private fun applyForce(value: String, matchedLength: Int, reversed: Boolean) {
        for (i in 0 until matchedLength) {
            val blockIdx = realIndex(reversed, stringBlock, i)
            val valueIdx = realIndex(reversed, value, i)
            if (flags.isCaseable(stringBlock[blockIdx])) {
                caseChoices[blockIdx] = value[valueIdx].isUpperCase()
            }
        }
    }

    /**
     * How many of [stringBlock]'s leading characters match [value]'s leading characters,
     * case-insensitively wherever [flags] allows it. 0 when a character does not match,
     * one of the strings must be consumed completely.
     * @see [RxAbsorbable.absorbableCount]
     */
    override fun absorbableCount(value: String): Int = matchCount(value, reversed = false)

    /**
     * True only when [stringBlock] is empty, as a non-empty literal can never render "".
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean = stringBlock.isEmpty()

    /**
     * Commits the matching leading characters' case to match [value]; mirrors
     * [absorbableCount] exactly.
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        val n = matchCount(value, reversed = false)
        require(n == stringBlock.length || n == value.length || n == 0)
        applyForce(value, matchedLength = n, reversed = false)
        return n
    }

    /**
     * No-op: only reachable when [stringBlock] is empty, so there's nothing to place.
     * @see [RxAbsorbable.forceZeroWidth]
     */
    override fun forceZeroWidth() {
        require(canBeZeroWidth)
    }

    /**
     * Suffix-anchored mirror of [absorbableCount]: how many of [stringBlock]'s trailing characters
     * match [value]'s trailing characters, case-insensitively wherever [flags] allows it.
     * 0 when a character does not match, one of the strings must be consumed completely.
     * @see [RxAbsorbable.absorbableSuffixCount]
     */
    override fun absorbableSuffixCount(value: String): Int = matchCount(value, reversed = true)

    /**
     * Suffix-anchored mirror of [tryForce]: Commits the matching trailing characters' case to
     * match [value]; mirrors [absorbableSuffixCount] exactly.
     * @see [RxAbsorbable.tryForceSuffix]
     */
    override fun tryForceSuffix(value: String): Int {
        require(value.isNotEmpty())
        val n = matchCount(value, reversed = true)
        require(n == stringBlock.length || n == value.length || n == 0)
        applyForce(value, matchedLength = n, reversed = true)
        return n
    }
}
