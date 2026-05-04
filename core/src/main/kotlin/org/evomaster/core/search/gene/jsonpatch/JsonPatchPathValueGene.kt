package org.evomaster.core.search.gene.jsonpatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene

/**
 * JSON Patch operation gene for operations that require "op", "path", and "value" fields.
 * Used for: add, replace, test.
 *
 * The path-value pair is a ChoiceGene over PairGene<EnumGene<String>, Gene>, where each pair
 * groups all schema paths of the same field type (first = path enum, second = typed value gene).
 * Switching the active choice switches both the set of valid paths and the value gene type,
 * keeping path and value type-compatible at all times.
 */
class JsonPatchPathValueGene(
    name: String,
    operationName: String,
    val pathValueChoice: ChoiceGene<PairGene<EnumGene<String>, Gene>>
) : JsonPatchOperationGene(name, operationName, listOf(pathValueChoice)) {

    init {
        require(operationName in listOf(JsonPatchOperationGene.OP_ADD, JsonPatchOperationGene.OP_REPLACE, JsonPatchOperationGene.OP_TEST)) {
            "JsonPatchPathValueGene only supports 'add', 'replace' or 'test', got: $operationName"
        }
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val activePair = pathValueChoice.activeGene()
        return formatOperation(
            mode,
            OpField("path", activePair.first.getValueAsRawString()),
            // value is unquoted in JSON since it can be any type (string, number, boolean, etc.)
            OpField("value", activePair.second.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck), quoted = false)
        )
    }

    override fun copyContent(): Gene =
        JsonPatchPathValueGene(
            name,
            operationName,
            pathValueChoice.copy() as ChoiceGene<PairGene<EnumGene<String>, Gene>>
        )

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchPathValueGene) throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        return operationName == other.operationName &&
                pathValueChoice.containsSameValueAs(other.pathValueChoice)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchPathValueGene) return false
        if (operationName != other.operationName) return false
        return pathValueChoice.unsafeCopyValueFrom(other.pathValueChoice)
    }
}