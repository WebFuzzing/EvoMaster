package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils

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

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (mode == GeneUtils.EscapeMode.XML) {
            val from = fromGene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.XML, targetFormat, extraCheck)
            val path = pathGene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.XML, targetFormat, extraCheck)
            return "<operation><op>$operationName</op><from>$from</from><path>$path</path></operation>"
        }
        // EnumGene<String>.getValueAsPrintableString() returns the raw string (no surrounding quotes),
        // so the manual "..." wrapping below produces valid JSON.
        val from = fromGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        return "{\"op\":\"$operationName\",\"from\":\"$from\",\"path\":\"$path\"}"
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