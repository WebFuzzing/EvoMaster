package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene

/**
 * JSON Patch operation gene for operations that require "op", "from", and "path" fields.
 * Used for: move, copy.
 */
class JsonPatchFromPathGene(
    name: String,
    operationName: String,
    val fromGene: EnumGene<String>,
    val pathGene: EnumGene<String>
) : JsonPatchOperationGene(name, operationName, listOf(fromGene, pathGene)) {

    init {
        require(operationName == JsonPatchOperationGene.OP_MOVE || operationName == JsonPatchOperationGene.OP_COPY) {
            "JsonPatchFromPathGene only supports 'move' or 'copy', got: $operationName"
        }
    }

    override fun copyContent(): Gene =
        JsonPatchFromPathGene(
            name,
            operationName,
            fromGene.copy() as EnumGene<String>,
            pathGene.copy() as EnumGene<String>
        )

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchFromPathGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return operationName == other.operationName &&
                fromGene.containsSameValueAs(other.fromGene) &&
                pathGene.containsSameValueAs(other.pathGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchFromPathGene) return false
        if (operationName != other.operationName) return false
        // Evaluate both before returning so neither field is skipped on partial failure.
        val fromOk = fromGene.unsafeCopyValueFrom(other.fromGene)
        val pathOk = pathGene.unsafeCopyValueFrom(other.pathGene)
        return fromOk && pathOk
    }
}