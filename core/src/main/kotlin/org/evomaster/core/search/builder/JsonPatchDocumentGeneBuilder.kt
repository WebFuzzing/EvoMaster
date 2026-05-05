package org.evomaster.core.search.gene.builder

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
import org.evomaster.core.search.service.Randomness

/**
 * Builds a JSON Patch document gene (RFC 6902).
 *
 * When [resourceSchema] is provided, paths and value gene types are derived from the schema.
 * Otherwise, random path strings are generated using [randomness] (or a fresh [Randomness]
 * if none is supplied), and both [StringGene] and [IntegerGene] are offered as value choices.
 */
object JsonPatchDocumentGeneBuilder {

    const val MIN_SIZE = 1
    const val DEFAULT_MAX_SIZE = 10
    private const val RANDOM_PATH_COUNT = 4

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
     * - Otherwise: paths are generated randomly via [randomness] (a fresh [Randomness] is used
     *   when none is supplied), and value choices are [StringGene] + [IntegerGene].
     */
    fun buildOperationsArray(
        resourceSchema: Gene? = null,
        randomness: Randomness? = null
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {

        val schemaFields = resourceSchema?.let { extractSchemaFields(it) }.orEmpty()

        return if (schemaFields.isNotEmpty()) {
            buildFromSchemaFields(schemaFields)
        } else {
            buildFromPaths(generateRandomPaths(randomness ?: Randomness()))
        }
    }

    // ---------------------------------------------------------------------------
    // private helpers
    // ---------------------------------------------------------------------------

    private fun generateRandomPaths(randomness: Randomness): List<String> =
        generateSequence { "/${randomness.nextWordString(2, 6)}" }
            .take(RANDOM_PATH_COUNT * 2)
            .distinct()
            .take(RANDOM_PATH_COUNT)
            .toList()
            .ifEmpty { listOf("/field") }

    private fun buildFromSchemaFields(
        fields: List<SchemaField>
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {
        val allPaths = fields.map { it.path }
        val pathEnum = EnumGene<String>("path", allPaths)

        val pathValueEntries: List<PairGene<EnumGene<String>, Gene>> =
            fields.groupBy { it.gene::class }
                .entries
                .mapIndexed { index, (_, group) ->
                    PairGene(
                        "entry_$index",
                        EnumGene("path", group.map { it.path }),
                        group.first().gene.copy()
                    )
                }

        return assemble(pathEnum, pathValueEntries)
    }

    /** No-schema case: both [StringGene] and [IntegerGene] offered as value choices. */
    private fun buildFromPaths(
        paths: List<String>
    ): ArrayGene<ChoiceGene<JsonPatchOperationGene>> {
        val pathEnum = EnumGene<String>("path", paths)
        val entries = listOf<PairGene<EnumGene<String>, Gene>>(
            PairGene("entry_string", pathEnum.copy() as EnumGene<String>, StringGene("value")),
            PairGene("entry_int",    pathEnum.copy() as EnumGene<String>, IntegerGene("value"))
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