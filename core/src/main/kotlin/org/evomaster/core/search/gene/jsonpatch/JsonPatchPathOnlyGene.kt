package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils

/**
 * JSON Patch operation gene for operations that only require an "op" and "path" field.
 * Currently used for: remove.
 */
class JsonPatchPathOnlyGene(
    name: String,
    operationName: String,
    val pathGene: EnumGene<String>
) : JsonPatchOperationGene(name, operationName, listOf(pathGene)) {

    init {
        require(operationName == "remove") {
            "JsonPatchPathOnlyGene only supports 'remove', got: $operationName"
        }
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (mode == GeneUtils.EscapeMode.XML) {
            val path = pathGene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.XML, targetFormat, extraCheck)
            return "<operation><op>$operationName</op><path>$path</path></operation>"
        }
        val path = pathGene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        return "{\"op\":\"$operationName\",\"path\":$path}"
    }

    override fun copyContent(): Gene =
        JsonPatchPathOnlyGene(name, operationName, pathGene.copy() as EnumGene<String>)

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchPathOnlyGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return pathGene.containsSameValueAs(other.pathGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchPathOnlyGene) return false
        return pathGene.unsafeCopyValueFrom(other.pathGene)
    }
}