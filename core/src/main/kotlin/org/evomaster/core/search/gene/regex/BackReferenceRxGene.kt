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
 * Its value is always identical to the current value of its [captureGroup].
 * It has no independent state and is therefore immutable.
 */
class BackReferenceRxGene(
    val groupIndex: Int,
    val captureGroup: DisjunctionListRxGene
) : RxAtom, SimpleGene("\\$groupIndex") {

    override fun checkForLocallyValidIgnoringChildren(): Boolean = true

    /**
     * Immutable, value is entirely determined by the referenced capture group.
     */
    override fun isMutable(): Boolean = false

    override fun copyContent(): Gene =
        BackReferenceRxGene(groupIndex, captureGroup)

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
    ): String = captureGroup.getValueAsPrintableString(targetFormat = null)

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BackReferenceRxGene) return false
        return captureGroup.getValueAsPrintableString(targetFormat = null) ==
                other.captureGroup.getValueAsPrintableString(targetFormat = null)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        // nothing to copy, as the value comes from the capture group
        return containsSameValueAs(other)
    }
}