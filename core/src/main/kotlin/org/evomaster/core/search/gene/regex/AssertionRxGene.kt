package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Distinguishes which direction an [AssertionRxGene] forces a candidate during repair.
 */
enum class AssertionType { LOOKAHEAD, LOOKBEHIND }

/**
 * Represents a zero-width assertion in the regex gene tree.
 *
 * This gene is placed in the tree at the position where the assertion appeared in the
 * source regex. It produces no characters ([getValueAsPrintableString] always returns
 * "").
 *
 * Repair is triggered from [DisjunctionRxGene.attemptAssertionRepair], invoked by
 * [RegexGene.randomize] after the disjunction's own sampled value is checked against
 * the source pattern and found not to match. [assertionType] determines which side of
 * the enclosing disjunction's terms the candidate gets forced onto. This gene's own
 * methods stay direction-agnostic.
 */
class AssertionRxGene(
    /**
     * The assertion's inner disjunction gene, can be null as the disjunction can be unsatisfiable,
     * in that case [innerGene] is null.
     */
    val innerGene: DisjunctionListRxGene?,
    val assertionType: AssertionType
) : RxTerm, CompositeFixedGene("assertion:${assertionType.name}", listOfNotNull(innerGene)) {

    /**
     *  To handle null [innerGene], in which case the assertion is unsatisfiable.
     */
    override fun canBeChildless() = true

    override fun checkForLocallyValidIgnoringChildren(): Boolean = true

    override fun isUnsatisfiable(): Boolean = innerGene == null

    override fun isMutable(): Boolean = innerGene?.isMutable() ?: false

    override fun copyContent(): Gene {
        val copy = AssertionRxGene(innerGene?.copy() as? DisjunctionListRxGene, assertionType)
        copy.name = this.name
        return copy
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        innerGene?.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
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
        if (innerGene == null) {
            return null
        }
        return innerGene.getValueAsPrintableString(targetFormat = null)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AssertionRxGene) {
            return false
        }
        if (assertionType != other.assertionType) {
            return false
        }
        return sampledInnerValue() == other.sampledInnerValue()
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is AssertionRxGene || assertionType != other.assertionType) {
            return false
        }
        return if (innerGene != null && other.innerGene != null) {
            innerGene.unsafeCopyValueFrom(other.innerGene)
        } else {
            innerGene == null && other.innerGene == null
        }
    }

    /**
     * Always true: an assertion never renders characters ([getValueAsPrintableString] is
     * always ""), so it can always collapse to zero width.
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean = true

    /**
     * Always 0: an assertion never absorbs candidate text into itself, it only supplies a
     * candidate for its siblings to absorb, via [sampledInnerValue].
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        return 0
    }

    /**
     * No-op: already always zero-width, so there's nothing to force.
     * @see [RxAbsorbable.forceZeroWidth]
     */
    override fun forceZeroWidth() {
        // already always zero-width, nothing to do
    }
}