package org.evomaster.core.search.gene.patch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * JSON Patch operation gene for operations that require "op", "from", and "path" fields.
 * Used for: move, copy.
 */
class JsonPatchFromPathGene(
    operationName: String,
    val fromGene: EnumGene<String>,
    val pathGene: EnumGene<String>,
    geneName: String = "${operationName}Op"
) : JsonPatchOperationGene(geneName, operationName, listOf(fromGene, pathGene)) {

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val from = fromGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        return "{\"op\":\"$operationName\",\"from\":$from,\"path\":$path}"
    }

    override fun copyContent(): Gene =
        JsonPatchFromPathGene(
            operationName,
            fromGene.copy() as EnumGene<String>,
            pathGene.copy() as EnumGene<String>,
            name
        )

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchFromPathGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return fromGene.containsSameValueAs(other.fromGene) && pathGene.containsSameValueAs(other.pathGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchFromPathGene) return false
        return fromGene.unsafeCopyValueFrom(other.fromGene) && pathGene.unsafeCopyValueFrom(other.pathGene)
    }
}