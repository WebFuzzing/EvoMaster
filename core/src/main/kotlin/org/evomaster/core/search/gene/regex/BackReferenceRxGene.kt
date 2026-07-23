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
 * Represents a backreference \N in a regex (N being a number).
 * Its value is always identical to the current value of its [captureGroup].
 * It has no independent state and is therefore immutable.
 * If capture group is null then the referenced group was unsatisfiable,
 * in which case the same is true for the backreference to it.
 */
class BackReferenceRxGene(
    val groupIndex: Int,
    val captureGroup: DisjunctionListRxGene?
) : RxAtom, CompositeFixedGene("\\$groupIndex", listOfNotNull(captureGroup)) {

    /**
     *  To handle null [captureGroup], in which case the back reference is unsatisfiable.
     */
    override fun canBeChildless() = true

    override fun isUnsatisfiable(): Boolean {
        return captureGroup == null || captureGroup.isUnsatisfiable()
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean = true

    /**
     * Immutable, value is entirely determined by the referenced capture group.
     */
    override fun isMutable(): Boolean = false

    override fun copyContent(): Gene {
        val copy = BackReferenceRxGene(groupIndex, captureGroup)
        copy.name = this.name //in case name is changed from its default
        return copy
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        throw IllegalStateException("Cannot randomize a BackReferenceRxGene, randomize the capture group instead.")
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
        throw IllegalStateException("Cannot mutate a BackReferenceRxGene, mutate the capture group .")
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (captureGroup == null) {
            throw IllegalStateException("Cannot get value from invalid backreference \\$groupIndex")
        }
        return captureGroup.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BackReferenceRxGene) return false
        return captureGroup == other.captureGroup
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
     * Always 0: a backreference's value is derived entirely from a previous [captureGroup],
     * so unlike an ordinary leaf it can not be forced to absorb arbitrary candidate text.
     * @see [RxAbsorbable.tryForce]
     */
    override fun tryForce(value: String): Int {
        require(value.isNotEmpty())
        return 0
    }
}