package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.builder.JsonPatchDocumentGeneBuilder
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Represents a JSON Patch document (RFC 6902) as a gene.
 *
 * The document is an array of patch operations. Each operation is represented by
 * a ChoiceGene that selects among the six standard operations:
 *   remove, move, copy, add, replace, test.
 *
 * [resourceSchema] is stored for future schema-aware path extraction but is not yet analysed.
 * Until that step is wired in, paths come from [JsonPatchDocumentGeneBuilder.DEFAULT_PATHS].
 *
 * The private constructor accepts a pre-built operations array (used by [copyContent]).
 */
class JsonPatchDocumentGene private constructor(
    name: String,
    val resourceSchema: Gene?,
    operationsArr: ArrayGene<ChoiceGene<JsonPatchOperationGene>>
) : CompositeFixedGene(name, listOf(operationsArr)) {

    constructor(name: String, resourceSchema: Gene? = null)
            : this(name, resourceSchema, JsonPatchDocumentGeneBuilder.buildOperationsArray(resourceSchema))

    companion object {
        val MIN_SIZE get() = JsonPatchDocumentGeneBuilder.MIN_SIZE
        val DEFAULT_MAX_SIZE get() = JsonPatchDocumentGeneBuilder.DEFAULT_MAX_SIZE
    }

    private val operationsArray: ArrayGene<ChoiceGene<JsonPatchOperationGene>>
        get() = children[0] as ArrayGene<ChoiceGene<JsonPatchOperationGene>>

    /** The currently active operation genes across all array elements. */
    val operations: List<JsonPatchOperationGene>
        get() = operationsArray.getViewOfElements().map { it.activeGene() }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        operationsArray.randomize(randomness, tryToForceNewValue)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String = operationsArray.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)

    override fun copyContent(): Gene =
        JsonPatchDocumentGene(
            name,
            resourceSchema?.copy(),
            operationsArray.copy() as ArrayGene<ChoiceGene<JsonPatchOperationGene>>
        )

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchDocumentGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return operationsArray.containsSameValueAs(other.operationsArray)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchDocumentGene) return false
        return operationsArray.unsafeCopyValueFrom(other.operationsArray)
    }

    override fun checkForLocallyValidIgnoringChildren() = true

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false
}