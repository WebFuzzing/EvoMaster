package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionKeyBuilder
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.BigDecimalGene

/**
 * A typed attribute gene belonging to a DynamoDB item.
 *
 * @property attributeName name of the DynamoDB item attribute
 * @property type native DynamoDB attribute type
 * @property gene evolvable value for the attribute
 */
data class DynamoDbAttributeGene(
    val attributeName: String,
    val type: DynamoDbValueTypeDto,
    val gene: Gene
) {
    /**
     * Converts scalar key metadata at the attribute boundary.
     *
     * @param attributeName name of the key attribute
     * @param type scalar key schema type
     * @param gene evolvable value for the attribute
     */
    constructor(attributeName: String, type: DynamoDbKeyScalarTypeDto, gene: Gene) :
        this(attributeName, DynamoDbValueTypeDto.fromKeyScalar(type), gene)

    /**
     * Converts this attribute gene into a typed controller DTO.
     *
     * @return typed attribute value DTO representing this gene
     */
    fun toDto(): DynamoDbAttributeValueDto =
        if (gene is DynamoDbTemplateGene) {
            gene.toDto()
        } else {
            DynamoDbAttributeValueDto(
                attributeName,
                type,
                when (type) {
                    DynamoDbValueTypeDto.STRING -> gene.getValueAsRawString()
                    DynamoDbValueTypeDto.NUMBER -> (gene as? BigDecimalGene)?.value?.toPlainString() ?: gene.getValueAsRawString()
                    DynamoDbValueTypeDto.BOOLEAN -> (gene as? BooleanGene)?.value?.toString() ?: gene.getValueAsRawString()
                    else -> gene.getValueAsRawString()
                }
            )
        }
}

/**
 * An initialization action that inserts one DynamoDB item.
 *
 * @property tableName target DynamoDB table
 * @param attributes item attributes to insert
 */
class DynamoDbAction(
    val tableName: String,
    attributes: List<DynamoDbAttributeGene>
) : EnvironmentAction(listOf()) {

    private val mutableAttributes = attributes.toMutableList()

    /** Immutable view of item attributes in this insertion action. */
    val attributes: List<DynamoDbAttributeGene> get() = mutableAttributes

    /**
     * Replaces an attribute gene at the given index.
     *
     * @param index position of the attribute gene to replace
     * @param gene new replacement gene
     */
    fun replaceAttributeGene(index: Int, gene: Gene) {
        killChild(mutableAttributes[index].gene)
        mutableAttributes[index] = mutableAttributes[index].copy(gene = gene)
        addChild(index, gene)
    }

    init {
        addChildren(attributes.map { it.gene })
    }

    /**
     * Returns the genes that determine the inserted item values.
     *
     * @return list of top-level attribute genes
     */
    override fun seeTopGenes(): List<Gene> = attributes.map { it.gene }

    /**
     * Creates an independent action with copies of all attribute genes.
     *
     * @return deep copy of this insertion action
     */
    override fun copyContent(): Action = DynamoDbAction(
        tableName,
        attributes.map { DynamoDbAttributeGene(it.attributeName, it.type, it.gene.copy()) }
    )

    /**
     * Returns the descriptive name of this insertion action.
     *
     * @return action name formatted with the table name
     */
    override fun getName(): String = "DynamoDB_INSERT_$tableName"

    /**
     * Returns the grouping key for DynamoDB initialization actions.
     *
     * @return fully qualified class name grouping key
     */
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
