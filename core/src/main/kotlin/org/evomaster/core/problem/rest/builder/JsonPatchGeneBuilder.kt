package org.evomaster.core.problem.rest.builder

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.evomaster.core.search.gene.wrapper.OptionalGene

/**
 * Builder for JSON Patch document genes.
 *
 * Provides heuristics that extract valid JSON Pointer paths from a resource schema
 * and group them by field type to construct type-safe path-value pairs.
 */
object JsonPatchGeneBuilder {

    /**
     * Represents a JSON Pointer path extracted from a resource schema,
     * paired with a value gene of the appropriate type for that field.
     * [depth] indicates nesting level: 1 = top-level field, 2 = one level nested, etc.
     */
    data class PathInfo(val path: String, val gene: Gene, val depth: Int = 1)

    /**
     * Returns true when [schema] contains only primitive leaf fields (no nested
     * ObjectGene or ArrayGene). Flat schemas only ever produce depth-1 paths.
     */
    fun isSchemaFlat(schema: Gene?): Boolean {
        val obj = unwrap(schema) as? ObjectGene ?: return false
        return obj.fields.all { field ->
            val inner = unwrap(field)
            inner !is ObjectGene && inner !is ArrayGene<*>
        }
    }

    /**
     * Recursively extracts all leaf-level JSON Pointer paths from [schema],
     * pairing each path with a fresh value gene whose type matches the schema field.
     *
     * - Nested objects produce sub-paths (e.g., /address/city).
     * - Optional and nullable wrappers are unwrapped transparently.
     * - Array fields and unrecognised gene types are skipped.
     * - When [schema] is flat, only depth-1 paths are returned regardless of [maxDepth].
     * - [maxDepth] caps recursion for nested schemas (default: unlimited).
     */
    fun extractAllPaths(schema: Gene?, prefix: String = "", maxDepth: Int = Int.MAX_VALUE): List<PathInfo> {
        if (schema == null) return emptyList()
        val effectiveMax = if (isSchemaFlat(schema)) 1 else maxDepth
        return extractPathsRecursive(schema, prefix, currentDepth = 0, maxDepth = effectiveMax)
    }

    private fun extractPathsRecursive(
        schema: Gene,
        prefix: String,
        currentDepth: Int,
        maxDepth: Int
    ): List<PathInfo> = when (schema) {
        is ObjectGene -> schema.fields.flatMap { field ->
            val fieldPath = "$prefix/${field.name}"
            val nextDepth = currentDepth + 1
            val innerPaths = if (nextDepth < maxDepth) {
                extractPathsRecursive(field, fieldPath, nextDepth, maxDepth)
            } else emptyList()
            if (innerPaths.isEmpty()) {
                val valueGene = createValueGene(field) ?: return@flatMap emptyList()
                listOf(PathInfo(fieldPath, valueGene, nextDepth))
            } else {
                innerPaths
            }
        }
        is OptionalGene -> extractPathsRecursive(schema.gene, prefix, currentDepth, maxDepth)
        is NullableGene -> extractPathsRecursive(schema.gene, prefix, currentDepth, maxDepth)
        else -> emptyList()
    }

    private fun unwrap(gene: Gene?): Gene? {
        var g = gene
        while (g is OptionalGene || g is NullableGene) {
            g = if (g is OptionalGene) g.gene else (g as NullableGene).gene
        }
        return g
    }

    /**
     * Groups [paths] by their value gene class and creates one [PairGene] per group.
     *
     * Within each group all paths share the same schema field type, so they can be
     * represented by a single EnumGene (first = path) and a single typed value gene (second).
     * The pair name follows the pattern "entry_N".
     *
     * Returns an empty list if [paths] is empty.
     */
    fun buildPathValueEntries(paths: List<PathInfo>): List<PairGene<EnumGene<String>, Gene>> {
        if (paths.isEmpty()) return emptyList()

        return paths
            .groupBy { it.gene.javaClass }
            .entries
            .mapIndexed { idx, (_, infos) ->
                val pathEnum = EnumGene<String>("path", infos.map { it.path })
                val valueGene = infos.first().gene.copy().also { it.name = "value" }
                PairGene<EnumGene<String>, Gene>("entry_$idx", pathEnum, valueGene)
            }
    }

    /**
     * Creates a fresh value gene whose type corresponds to [fieldGene].
     * Returns null for composite types (like ObjectGene) that are handled recursively
     * by [extractAllPaths] rather than as leaf values.
     */
    internal fun createValueGene(fieldGene: Gene): Gene? {
        val gene = unwrap(fieldGene) ?: return null
        return when (gene) {
            is StringGene     -> StringGene("value")
            is IntegerGene    -> IntegerGene("value")
            is LongGene       -> LongGene("value")
            is FloatGene      -> FloatGene("value")
            is DoubleGene     -> DoubleGene("value")
            is BigDecimalGene -> BigDecimalGene("value")
            is BigIntegerGene -> BigIntegerGene("value")
            is BooleanGene    -> BooleanGene("value")
            is ObjectGene     -> null
            else              -> null
        }
    }
}