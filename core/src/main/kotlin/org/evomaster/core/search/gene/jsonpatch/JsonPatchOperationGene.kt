package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Base class for all JSON Patch operation genes.
 * Each operation gene knows its operation name (op) and carries the relevant field genes.
 *
 * Serialization for all subtypes lives here: the when(this) block is intentional — this is a
 * closed hierarchy with exactly three known subtypes, so the parent knowing their structure
 * keeps all printing logic in one place.
 */
abstract class JsonPatchOperationGene(
    name: String,
    val operationName: String,
    children: List<Gene>
) : CompositeFixedGene(name, children) {

    companion object {
        const val OP_ADD     = "add"
        const val OP_REMOVE  = "remove"
        const val OP_REPLACE = "replace"
        const val OP_MOVE    = "move"
        const val OP_COPY    = "copy"
        const val OP_TEST    = "test"
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val fields = when (this) {
            is JsonPatchPathOnlyGene -> {
                val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                if (mode == GeneUtils.EscapeMode.XML) "<path>$path</path>"
                else "\"path\":\"$path\""
            }
            is JsonPatchFromPathGene -> {
                val from = fromGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                if (mode == GeneUtils.EscapeMode.XML) "<from>$from</from><path>$path</path>"
                else "\"from\":\"$from\",\"path\":\"$path\""
            }
            is JsonPatchPathValueGene -> {
                val pair = pathValueChoice.activeGene()
                val path = pair.first.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                // value is unquoted in JSON since it can be any type (string, number, boolean, etc.)
                val value = pair.second.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
                if (mode == GeneUtils.EscapeMode.XML) "<path>$path</path><value>$value</value>"
                else "\"path\":\"$path\",\"value\":$value"
            }
            else -> throw IllegalStateException("Unknown JsonPatchOperationGene subtype: ${this::class}")
        }
        return if (mode == GeneUtils.EscapeMode.XML)
            "<operation><op>$operationName</op>$fields</operation>"
        else
            "{\"op\":\"$operationName\",$fields}"
    }

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        children.filter { it.isMutable() }.forEach { it.randomize(randomness, tryToForceNewValue) }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false
}