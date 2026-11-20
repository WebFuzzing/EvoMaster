package org.evomaster.core.search.gene.dynamodb

import org.evomaster.core.database.dynamodb.DynamoDbValueType
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Fixed-size composite gene representing a non-empty DynamoDB string set (`SS`) or number set (`NS`).
 *
 * Preserves DynamoDB set semantics independently from lists ([org.evomaster.core.search.gene.collection.TupleGene]),
 * including when nested inside maps or lists.
 *
 * @property setType native set type ([DynamoDbValueType.STRING_SET] or [DynamoDbValueType.NUMBER_SET])
 * @property elements member genes representing the set elements
 */
class DynamoDbSetGene(
    name: String,
    val setType: DynamoDbValueType,
    val elements: List<Gene>
) : CompositeFixedGene(name, elements) {

    init {
        require(setType == DynamoDbValueType.STRING_SET || setType == DynamoDbValueType.NUMBER_SET) {
            "DynamoDbSetGene only supports STRING_SET and NUMBER_SET, got $setType"
        }
        require(elements.isNotEmpty()) {
            "DynamoDB sets cannot be empty"
        }
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean = elements.isNotEmpty()

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        elements.filter { it.isMutable() }.forEach {
            it.randomize(randomness, false)
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DynamoDbSetGene || setType != other.setType || elements.size != other.elements.size) {
            return false
        }
        return elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.javaClass == otherElem.javaClass && thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        val gene = other.getPhenotype()
        if (gene is DynamoDbSetGene
            && setType == gene.setType
            && elements.size == gene.elements.size
            && elements.indices.all { elements[it].possiblySame(gene.elements[it]) }
        ) {
            var result = true
            elements.indices.forEach {
                if (!elements[it].unsafeCopyValueFrom(gene.elements[it])) {
                    result = false
                }
            }
            return result
        }
        return false
    }

    override fun isMutable(): Boolean = elements.any { it.isMutable() }

    override fun mutationWeight(): Double = elements.sumOf { it.mutationWeight() }

    override fun copyContent(): Gene = DynamoDbSetGene(name, setType, elements.map(Gene::copy))

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean = false

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "[" + elements.filter { it.isPrintable() }.joinToString(", ") {
            it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        } + "]"
    }
}
