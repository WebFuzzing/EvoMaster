package org.evomaster.core.search.gene.builder

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchFromPathGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchOperationGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathOnlyGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene

/**
 * Builds a JSON Patch document gene (RFC 6902).
 *
 * [resourceSchema] is accepted but not yet analysed; schema-aware path extraction
 * will be wired in a future step. Until then, [DEFAULT_PATHS] are used as placeholders.
 */
object JsonPatchDocumentGeneBuilder {

    const val MIN_SIZE = 1
    const val DEFAULT_MAX_SIZE = 10

    val DEFAULT_PATHS = listOf("/", "/a", "/b", "/c")

    /**
     * Builds the ArrayGene of patch operations.
     *
     * All six RFC 6902 operation choices (remove, move, copy, add, replace, test) are always
     * present in the ChoiceGene template so that mutation can switch freely between them.
     * [resourceSchema] is reserved for future schema-based path extraction and is ignored for now.
     */
    fun buildOperationsArray(
        // TODO: resourceSchema is ignored in this PR — path extraction will be wired in a follow-up
        resourceSchema: Gene? = null,
        paths: List<String> = DEFAULT_PATHS
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {

        val effectivePaths = paths.ifEmpty { listOf("/") }
        val pathEnum = EnumGene<String>("path", effectivePaths)

        val choices = mutableListOf<JsonPatchOperationGene>()

        choices.add(JsonPatchPathOnlyGene(JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE, pathEnum.copy() as EnumGene<String>))

        choices.add(
            JsonPatchFromPathGene(
                JsonPatchOperationGene.OP_MOVE,
                JsonPatchOperationGene.OP_MOVE,
                fromGene = pathEnum.copy() as EnumGene<String>,
                pathGene  = pathEnum.copy() as EnumGene<String>
            )
        )
        choices.add(
            JsonPatchFromPathGene(
                JsonPatchOperationGene.OP_COPY,
                JsonPatchOperationGene.OP_COPY,
                fromGene = pathEnum.copy() as EnumGene<String>,
                pathGene  = pathEnum.copy() as EnumGene<String>
            )
        )

        // TODO: replace StringGene with a schema-aware gene derived from resourceSchema
        //  (e.g. IntegerGene, BooleanGene, ObjectGene) once path extraction is wired in
        val entryTemplate = PairGene<EnumGene<String>, Gene>(
            "entry_0",
            pathEnum.copy() as EnumGene<String>,
            StringGene("value")
        )

        for (op in listOf(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_TEST)) {
            choices.add(
                JsonPatchPathValueGene(
                    op,
                    op,
                    ChoiceGene("${op}PathValue", listOf(entryTemplate.copy() as PairGene<EnumGene<String>, Gene>))
                )
            )
        }

        val template = ChoiceGene<JsonPatchOperationGene>("operation", choices)
        return ArrayGene("operations", template, minSize = MIN_SIZE, maxSize = DEFAULT_MAX_SIZE)
    }
}