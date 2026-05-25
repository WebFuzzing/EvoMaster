package org.evomaster.core.search.gene.builder

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchFromPathGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchOperationGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathOnlyGene
import org.evomaster.core.search.gene.jsonpatch.JsonPatchPathValueGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene

/**
 * Builds a JSON Patch document gene (RFC 6902).
 *
 * When [resourceSchema] is provided, paths and value gene types are derived from the schema.
 * Otherwise, a fixed deterministic default template is used: paths /a, /b, /c, /d with
 * String, Integer, and Boolean value types.
 */
object JsonPatchDocumentGeneBuilder {

    const val MIN_SIZE = 1
    const val DEFAULT_MAX_SIZE = 10

    const val FIELD_PATH = "path"
    const val FIELD_FROM = "from"
    const val FIELD_VALUE = "value"

    const val ENTRY_STRING = "entry_string"
    private const val ENTRY_INT = "entry_int"
    private const val ENTRY_BOOL = "entry_bool"

    val DEFAULT_PATHS = listOf("/a", "/b", "/c", "/d")

    /** A single patchable field extracted from a resource schema. */
    internal data class SchemaField(val path: String, val gene: Gene)

    /**
     * Walks [schema] recursively and returns one [SchemaField] per reachable leaf field,
     * using JSON Pointer notation for paths (e.g. "/name", "/address/street").
     *
     * - [ObjectGene]: descends into each fixed field.
     * - [OptionalGene]: unwraps and descends using the same path prefix.
     * - [CycleObjectGene]: skipped to avoid infinite loops.
     * - Everything else (leaf genes and [ArrayGene]): treated as a patchable target at [prefix].
     */
    internal fun extractSchemaFields(schema: Gene, prefix: String = ""): List<SchemaField> {
        return when (schema) {
            is CycleObjectGene -> emptyList()
            is OptionalGene -> extractSchemaFields(schema.gene, prefix)
            is ObjectGene -> schema.fixedFields.flatMap { field ->
                extractSchemaFields(field, "$prefix/${field.name}")
            }
            else -> if (prefix.isNotEmpty()) listOf(SchemaField(prefix, schema.copy())) else emptyList()
        }
    }

    /**
     * Builds the ArrayGene of patch operations.
     *
     * All six RFC 6902 operation choices (remove, move, copy, add, replace, test) are always
     * present in the ChoiceGene template.
     *
     * - If [resourceSchema] yields fields: paths and typed value genes come from the schema.
     * - Otherwise: a fixed default set of paths (/a, /b, /c, /d) with String, Integer, and
     *   Boolean value types is used. This keeps the builder fully deterministic.
     */
    fun buildOperationsArray(
        resourceSchema: Gene? = null
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {

        val schemaFields = resourceSchema?.let { extractSchemaFields(it) }.orEmpty()

        return if (schemaFields.isNotEmpty()) {
            buildFromSchemaFields(schemaFields)
        } else {
            buildFromPaths(DEFAULT_PATHS)
        }
    }

    // ---------------------------------------------------------------------------
    // private helpers
    // ---------------------------------------------------------------------------

    private fun buildFromSchemaFields(
        fields: List<SchemaField>
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {
        val allPaths = fields.map { it.path }
        val pathEnum = EnumGene<String>(FIELD_PATH, allPaths)

        val pathValueEntries: List<PairGene<EnumGene<String>, Gene>> =
            fields.groupBy { it.gene::class }
                .entries
                .mapIndexed { index, (_, group) ->
                    PairGene(
                        "entry_$index",
                        EnumGene(FIELD_PATH, group.map { it.path }),
                        group.first().gene.copy()
                    )
                }

        return assemble(pathEnum, pathValueEntries)
    }

    /** No-schema case: String, Integer, and Boolean value types over the default fixed paths. */
    private fun buildFromPaths(
        paths: List<String>
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {
        val pathEnum = EnumGene<String>(FIELD_PATH, paths)
        val entries = listOf<PairGene<EnumGene<String>, Gene>>(
            PairGene(ENTRY_STRING, pathEnum.copy() as EnumGene<String>, StringGene(FIELD_VALUE)),
            PairGene(ENTRY_INT,    pathEnum.copy() as EnumGene<String>, IntegerGene(FIELD_VALUE)),
            PairGene(ENTRY_BOOL,   pathEnum.copy() as EnumGene<String>, BooleanGene(FIELD_VALUE))
        )
        return assemble(pathEnum, entries)
    }

    private fun assemble(
        pathEnum: EnumGene<String>,
        pathValueEntries: List<PairGene<EnumGene<String>, Gene>>
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {

        val choices = mutableListOf<JsonPatchOperationGene>()

        choices.add(
            JsonPatchPathOnlyGene(
                JsonPatchOperationGene.OP_REMOVE, JsonPatchOperationGene.OP_REMOVE,
                pathEnum.copy() as EnumGene<String>
            )
        )
        choices.add(
            JsonPatchFromPathGene(
                JsonPatchOperationGene.OP_MOVE, JsonPatchOperationGene.OP_MOVE,
                fromGene = pathEnum.copy() as EnumGene<String>,
                pathGene  = pathEnum.copy() as EnumGene<String>
            )
        )
        choices.add(
            JsonPatchFromPathGene(
                JsonPatchOperationGene.OP_COPY, JsonPatchOperationGene.OP_COPY,
                fromGene = pathEnum.copy() as EnumGene<String>,
                pathGene  = pathEnum.copy() as EnumGene<String>
            )
        )

        for (op in listOf(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_TEST)) {
            choices.add(
                JsonPatchPathValueGene(
                    op, op,
                    ChoiceGene("${op}PathValue", pathValueEntries.map {
                        it.copy() as PairGene<EnumGene<String>, Gene>
                    })
                )
            )
        }

        return ArrayGene("operations", ChoiceGene("operation", choices),
            minSize = MIN_SIZE, maxSize = DEFAULT_MAX_SIZE)
    }
}