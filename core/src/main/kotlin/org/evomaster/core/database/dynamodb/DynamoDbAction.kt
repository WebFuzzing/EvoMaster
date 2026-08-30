package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.Gene

/**
 * A typed attribute gene belonging to a DynamoDB item.
 *
 * @property attributeName name of the DynamoDB item attribute
 * @property type supported DynamoDB scalar type
 * @property gene evolvable value for the attribute
 */
data class DynamoDbAttributeGene(
    val attributeName: String,
    val type: DynamoDbScalarTypeDto,
    val gene: Gene
)

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

    /** Stable key used to avoid adding the same inferred insertion twice. */
    fun insertionKey(): String = buildString {
        append(tableName)
        attributes.forEach {
            append('|').append(it.attributeName).append(':').append(it.type)
                .append('=').append(it.gene.getValueAsRawString())
        }
    }
}
