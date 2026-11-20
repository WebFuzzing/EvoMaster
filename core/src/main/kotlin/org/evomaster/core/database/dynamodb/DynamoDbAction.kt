package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionKeyBuilder
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.TupleGene
import org.evomaster.core.search.gene.dynamodb.DynamoDbSetGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import java.math.BigDecimal
import java.util.TreeSet

/**
 * A typed attribute gene belonging to a DynamoDB item.
 *
 * @property attributeName name of the DynamoDB item attribute
 * @property type native DynamoDB attribute type
 * @property gene evolvable value for the attribute
 */
data class DynamoDbAttributeGene(
    val attributeName: String,
    val type: DynamoDbValueType,
    val gene: Gene
) {
    /**
     * Converts this attribute gene into a plain Java/Kotlin value (scalar, map, list, set, or null).
     */
    fun toPlainValue(): Any? = when (type) {
        DynamoDbValueType.STRING -> geneToPlainString(gene)
        DynamoDbValueType.NUMBER -> geneToPlainNumber(gene)
        DynamoDbValueType.BOOLEAN -> geneToPlainBoolean(gene)
        DynamoDbValueType.NULL -> null
        DynamoDbValueType.MAP, DynamoDbValueType.LIST -> geneToPlainValue(gene)
        DynamoDbValueType.STRING_SET -> geneToPlainSet(gene, ::geneToPlainString)
        DynamoDbValueType.NUMBER_SET -> geneToPlainSet(gene, ::geneToPlainNumber)
    }

    /**
     * Converts this attribute gene into a typed controller DTO.
     *
     * @return typed attribute value DTO representing this gene
     */
    fun toDto(): DynamoDbAttributeValueDto = when (type) {
        DynamoDbValueType.STRING -> DynamoDbAttributeValueDto(
            attributeName,
            DynamoDbValueTypeDto.STRING,
            geneToPlainString(gene)
        )
        DynamoDbValueType.NUMBER -> DynamoDbAttributeValueDto(
            attributeName,
            DynamoDbValueTypeDto.NUMBER,
            (gene as? BigDecimalGene)?.value?.toPlainString()
                ?: gene.getValueAsRawString()
        )
        DynamoDbValueType.BOOLEAN -> DynamoDbAttributeValueDto(
            attributeName,
            DynamoDbValueTypeDto.BOOLEAN,
            (gene as? BooleanGene)?.value?.toString()
                ?: gene.getValueAsRawString()
        )
        DynamoDbValueType.NULL -> DynamoDbDtoUtils.fromPlainValue(attributeName, null)
        DynamoDbValueType.MAP,
        DynamoDbValueType.LIST,
        DynamoDbValueType.STRING_SET,
        DynamoDbValueType.NUMBER_SET ->
            DynamoDbDtoUtils.fromPlainValue(attributeName, toPlainValue())
    }

    companion object {
        private fun geneToPlainString(gene: Gene): String = gene.getValueAsRawString()

        private fun geneToPlainNumber(gene: Gene): BigDecimal =
            (gene as? BigDecimalGene)?.value ?: BigDecimal(gene.getValueAsRawString())

        private fun geneToPlainBoolean(gene: Gene): Boolean =
            (gene as? BooleanGene)?.value ?: gene.getValueAsRawString().toBoolean()

        private fun <T : Comparable<T>> geneToPlainSet(
            gene: Gene,
            convert: (Gene) -> T
        ): Set<T> {
            val elements = when (gene) {
                is DynamoDbSetGene -> gene.elements
                is TupleGene -> gene.elements
                else -> listOf(gene)
            }
            val sorted = TreeSet<T>()
            elements.mapTo(sorted, convert)
            return LinkedHashSet(sorted)
        }

        private fun geneToPlainValue(gene: Gene): Any? = when (gene) {
            is ObjectGene -> linkedMapOf<String, Any?>().apply {
                gene.fixedFields.sortedBy { it.name }.forEach { put(it.name, geneToPlainValue(it)) }
            }
            is DynamoDbSetGene -> when (gene.setType) {
                DynamoDbValueType.STRING_SET -> geneToPlainSet(gene, ::geneToPlainString)
                DynamoDbValueType.NUMBER_SET -> geneToPlainSet(gene, ::geneToPlainNumber)
                else -> throw IllegalStateException("Unsupported DynamoDbSetGene type: ${gene.setType}")
            }
            is TupleGene -> gene.elements.map { geneToPlainValue(it) }
            is ImmutableDataHolderGene -> when (gene.value) {
                "null" -> null
                "[]" -> emptyList<Any?>()
                else -> throw IllegalStateException("Unexpected ImmutableDataHolderGene value: ${gene.value}")
            }
            is StringGene, is EnumGene<*> -> gene.getValueAsRawString()
            is BigDecimalGene -> gene.value
            is BooleanGene -> gene.value
            else -> gene.getValueAsRawString()
        }
    }
}

/**
 * An initialization action that inserts one DynamoDB item.
 *
 * @property tableName target DynamoDB table
 * @property attributes item attributes to insert
 */
class DynamoDbAction(
    val tableName: String,
    val attributes: List<DynamoDbAttributeGene>
) : EnvironmentAction(listOf()) {

    init {
        addChildren(attributes.map { it.gene })
    }

    /** Returns the genes that determine the inserted item values. */
    override fun seeTopGenes(): List<Gene> = attributes.map { it.gene }

    /** Creates an independent action with copies of all attribute genes. */
    override fun copyContent(): Action = DynamoDbAction(
        tableName,
        attributes.map { DynamoDbAttributeGene(it.attributeName, it.type, it.gene.copy()) }
    )

    /** Returns the descriptive name of this insertion action. */
    override fun getName(): String = "DynamoDB_INSERT_$tableName"

    /** Returns the grouping key for DynamoDB initialization actions. */
    override fun getActionGroupKey(): String = DynamoDbAction::class.java.name

    /**
     * Stable key used to avoid adding the same inferred insertion twice.
     *
     * @return canonical insertion key string
     */
    fun insertionKey(): String = DynamoDbInsertionKeyBuilder.fromAttributes(
        tableName,
        attributes.map { it.toDto() }
    )
}
