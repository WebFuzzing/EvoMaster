package org.evomaster.core.search.gene.patch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * JSON Patch operation gene for operations that only require an "op" and "path" field.
 * Currently used for: remove.
 */
class JsonPatchPathOnlyGene(
    operationName: String,
    val pathGene: EnumGene<String>,
    geneName: String = "${operationName}Op"
) : JsonPatchOperationGene(geneName, operationName, listOf(pathGene)) {

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        return "{\"op\":\"$operationName\",\"path\":$path}"
    }

    override fun copyContent(): Gene =
        JsonPatchPathOnlyGene(operationName, pathGene.copy() as EnumGene<String>, name)

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchPathOnlyGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return pathGene.containsSameValueAs(other.pathGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchPathOnlyGene) return false
        return pathGene.unsafeCopyValueFrom(other.pathGene)
    }
}