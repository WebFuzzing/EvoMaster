package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene

/** Builds evolvable DynamoDB insertion actions from failed equality reads. */
object DynamoDbInsertBuilder {

    /**
     * Builds unique, supported DynamoDB insertion actions from failed equality reads.
     *
     * @param failedQueries failed reads reported by the controller
     * @param existingInsertionKeys keys of insertions that have already been added to the individual
     * @return inferred actions not already represented by [existingInsertionKeys]
     */
    fun buildInsertActions(
        failedQueries: List<DynamoDbFailedQuery>,
        existingInsertionKeys: Set<String>
    ): List<DynamoDbAction> = failedQueries
        .mapNotNull(::toActionOrNull)
        .filterNot { it.insertionKey() in existingInsertionKeys }
        .distinctBy { it.insertionKey() }

    /** Converts one failed read into an insertion action, or returns null when it is incomplete. */
    private fun toActionOrNull(query: DynamoDbFailedQuery): DynamoDbAction? {
        val tableName = query.tableName
        val queryAttributes = query.attributes
        if (tableName.isNullOrBlank() || queryAttributes.isNullOrEmpty()) return null

        val attributes = queryAttributes.map { attribute ->
            toAttributeOrNull(attribute) ?: return null
        }

        return DynamoDbAction(tableName, attributes)
    }

    /** Converts a supported scalar DynamoDB attribute into its evolvable representation. */
    private fun toAttributeOrNull(attribute: DynamoDbAttributeValueDto): DynamoDbAttributeGene? {
        val type = attribute.type ?: return null
        val value = attribute.value ?: return null
        val gene = when (type) {
            DynamoDbScalarTypeDto.STRING -> StringGene(attribute.attributeName, value)
            DynamoDbScalarTypeDto.NUMBER -> value.toBigDecimalOrNull()?.let {
                BigDecimalGene(attribute.attributeName, it)
            } ?: return null
            DynamoDbScalarTypeDto.BOOLEAN -> BooleanGene(attribute.attributeName, value.toBoolean())
        }

        return DynamoDbAttributeGene(attribute.attributeName, type, gene)
    }
}
