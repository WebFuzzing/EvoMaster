package org.evomaster.core.search.gene.patch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.builder.JsonPatchGeneBuilder
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
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
 * When [resourceSchema] is provided, paths are extracted from the schema and value genes
 * are typed to match each field's schema type. Otherwise a single "/" fallback path is used.
 *
 * The primary constructor builds the operations array from [resourceSchema].
 * The private secondary constructor accepts a pre-built array, used internally by [copyContent].
 */
class JsonPatchDocumentGene private constructor(
    name: String,
    val resourceSchema: Gene?,
    operationsArr: ArrayGene<ChoiceGene<JsonPatchOperationGene>>
) : CompositeFixedGene(name, listOf(operationsArr)) {

    constructor(name: String, resourceSchema: Gene? = null)
            : this(name, resourceSchema, buildOperationsArray(resourceSchema))

    companion object {
        const val MIN_SIZE = 1
        const val DEFAULT_MAX_SIZE = 10

        /**
         * Builds the ArrayGene of patch operations from the resource schema.
         *
         * All six operation choices are always present in the ChoiceGene template so that
         * mutation can switch between them. Only the active choice is included in the phenotype.
         */
        fun buildOperationsArray(
            resourceSchema: Gene?
        ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {

            val allPaths = JsonPatchGeneBuilder.extractAllPaths(resourceSchema)
            // Without a schema there are no known paths; "/" (JSON Pointer root) is used as the
            // only fallback so that the EnumGene always has at least one selectable value.
            val pathList = if (allPaths.isEmpty()) listOf("/") else allPaths.map { it.path }
            val pathEnum = EnumGene<String>("path", pathList)

            val choices = mutableListOf<JsonPatchOperationGene>()

            // Operations that only need a path
            choices.add(JsonPatchPathOnlyGene("remove", pathEnum.copy() as EnumGene<String>))

            // Operations that need from + path
            choices.add(
                JsonPatchFromPathGene(
                    "move",
                    fromGene = pathEnum.copy() as EnumGene<String>,
                    pathGene = pathEnum.copy() as EnumGene<String>
                )
            )
            choices.add(
                JsonPatchFromPathGene(
                    "copy",
                    fromGene = pathEnum.copy() as EnumGene<String>,
                    pathGene = pathEnum.copy() as EnumGene<String>
                )
            )

            // Operations that need path + value; pairs are grouped by schema field type
            val pathValueEntries = JsonPatchGeneBuilder.buildPathValueEntries(allPaths)
            val effectiveEntries: List<PairGene<EnumGene<String>, Gene>> =
                if (pathValueEntries.isNotEmpty()) {
                    pathValueEntries
                } else {
                    // Fallback: no schema info — use a generic string value on the root path
                    listOf(PairGene("entry_0", EnumGene("path", listOf("/")), StringGene("value")))
                }

            for (op in listOf("add", "replace", "test")) {
                val entryCopies = effectiveEntries.map { it.copy() as PairGene<EnumGene<String>, Gene> }
                choices.add(JsonPatchPathValueGene(op, ChoiceGene("${op}PathValue", entryCopies)))
            }

            val template = ChoiceGene<JsonPatchOperationGene>("operation", choices)
            return ArrayGene("operations", template, minSize = MIN_SIZE, maxSize = DEFAULT_MAX_SIZE)
        }
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