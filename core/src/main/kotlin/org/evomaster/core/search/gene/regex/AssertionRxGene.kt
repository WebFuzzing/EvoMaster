package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Specifies what kind of zero-width assertion an [AssertionRxGene] represents.
 */
enum class AssertionType {
    /** (?=...) */
    LOOKAHEAD,
    // TODO implement more assertion types
}

/**
 * Represents a zero-width assertion in the regex gene tree.
 *
 * This gene is placed in the tree at the position where the assertion appeared in the
 * source regex. It produces no characters ([getValueAsPrintableString] always returns
 * "").
 *
 * Repair is triggered from [RegexGene.randomize] after the main tree is sampled and
 * validation against the source pattern fails. See [RegexGene.attemptAssertionRepair].
 */
class AssertionRxGene(
    val assertionType: AssertionType = AssertionType.LOOKAHEAD,
    val innerGene: DisjunctionListRxGene?
) : RxTerm, SimpleGene("assertion") {

    override fun checkForLocallyValidIgnoringChildren(): Boolean = true

    override fun isUnsatisfiable(): Boolean {
        return when (assertionType) {
            AssertionType.LOOKAHEAD -> innerGene == null
        }
    }

    override fun isMutable(): Boolean = innerGene?.isMutable() ?: false

    override fun copyContent(): Gene {
        val copy = AssertionRxGene(assertionType, innerGene?.copy() as? DisjunctionListRxGene)
        copy.name = this.name
        return copy
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        innerGene?.randomize(randomness, tryToForceNewValue)
    }

    override fun setValueWithRawString(value: String) {
        innerGene?.setFromStringValue(value)
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    /**
     * Zero-width: never contributes characters to the generated string.
     */
    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String = ""

    /**
     * Returns the value currently sampled from [innerGene], or null if there is no
     * inner gene to sample from.
     */
    fun sampledInnerValue(): String? {
        if (innerGene == null) return null
        return innerGene.getValueAsPrintableString(targetFormat = null)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AssertionRxGene) return false
        if (assertionType != other.assertionType) return false
        return sampledInnerValue() == other.sampledInnerValue()
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is AssertionRxGene || other.assertionType != assertionType) return false
        return if (innerGene != null && other.innerGene != null) {
            innerGene.unsafeCopyValueFrom(other.innerGene)
        } else {
            innerGene == null && other.innerGene == null
        }
    }

    override fun canBeZeroWidth(): Boolean = true

    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        return 0
    }
}