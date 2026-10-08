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
 * Represents a backreference \N in a regex (N being a number).
 * Its value is the one that the capture group number [groupIndex] has in the tree as it is rendered.
 * That is found when rendering, from the [RegexGene] this is part of, and not by keeping a reference to the group
 * (as this causes `.copy()` issues and problems with groups inside quantifiers).
 *
 * It has no independent state and is therefore immutable.
 *
 * If the group did not take part in what was rendered (or this is not part of a [RegexGene]), the value is "".
 * This is what JS does for non-participating referenced groups. For Java this would make the backref unsatisfiable
 * (as long as the group is not participating), but as that gets complex we can make its value empty and if that
 * causes a mismatch the regex verifier on [RegexGene] would re-randomize.
 *
 * @param groupIndex The number of the referenced group
 * @param unsatisfiable Whether this backreference can never match. In Java this happens when the captured group is
 * unsatisfiable, or when the reference points to a future group (or itself).
 */
class BackReferenceRxGene(
    val groupIndex: Int,
    private val unsatisfiable: Boolean = false
) : RxAtom, SimpleGene("\\$groupIndex") {

    override fun isUnsatisfiable(): Boolean {
        return unsatisfiable
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean = true

    /**
     * Immutable, value is entirely determined by the referenced capture group.
     */
    override fun isMutable(): Boolean = false

    override fun copyContent(): Gene {
        val copy = BackReferenceRxGene(groupIndex, unsatisfiable)
        copy.name = this.name //in case name is changed from its default
        return copy
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException(
            "Cannot set value directly on a BackReferenceRxGene, set the capture group instead."
        )
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        throw IllegalStateException("Cannot randomize a BackReferenceRxGene, randomize the capture group instead.")
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        throw IllegalStateException("Cannot mutate a BackReferenceRxGene, mutate the capture group .")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (unsatisfiable) {
            throw IllegalStateException("Cannot get value from invalid backreference \\$groupIndex")
        }
        return getFirstParent(RegexGene::class.java)?.getCapturedValue(groupIndex) ?: ""
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        // there is no state, the value comes from the group
        if (other !is BackReferenceRxGene) return false
        return groupIndex == other.groupIndex && unsatisfiable == other.unsatisfiable
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        // nothing to copy, as the value comes from the capture group
        return containsSameValueAs(other)
    }

    /**
     * Returns false as we do not want backreferences to mutate a previous group.
     * @see [RxAbsorbable.canBeZeroWidth]
     */
    override val canBeZeroWidth: Boolean = false

    /**
     * Always 0: a backreference's value is derived entirely from a previous capture group,
     * so unlike an ordinary leaf it can not be forced to absorb arbitrary candidate text.
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        return 0
    }
}