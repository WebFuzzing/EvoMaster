package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/** Builds evolvable DynamoDB insertion actions from failed equality reads. */
object DynamoDbInsertBuilder {

    /**
     * Builds an insertion for each supported failed read, as for MongoDB initialization.
     * Only absent primary-key values are randomized; inferred values retain their seeds.
     *
     * @param failedQueries failed reads reported by the controller
     * @param randomness search randomness used to initialize missing keys
     * @return inferred actions for the reported failed reads
     */
    fun buildInsertActions(
        failedQueries: List<DynamoDbFailedQuery>,
        randomness: Randomness
    ): List<DynamoDbAction> = failedQueries
        .mapNotNull { toActionOrNull(it, randomness) }

    /** Converts one failed read into an insertion action, or returns null when it is incomplete. */
    private fun toActionOrNull(query: DynamoDbFailedQuery, randomness: Randomness): DynamoDbAction? {
        val tableName = query.tableName
        val queryAttributes = query.attributes
        if (tableName.isNullOrBlank() || queryAttributes.isNullOrEmpty()) return null

        val attributes = queryAttributes.map { attribute ->
            toAttributeOrNull(attribute) ?: return null
        }

        val missingKeys = (query.missingPrimaryKeyAttributes ?: emptyMap()).map { (name, type) ->
            if (name.isNullOrBlank() || attributes.any { it.attributeName == name }) return null
            val gene = when (type) {
                DynamoDbScalarTypeDto.STRING -> StringGene(name, minLength = 1)
                // Seed whole numbers within DynamoDB's 38-digit precision limit.
                DynamoDbScalarTypeDto.NUMBER -> BigDecimalGene(name, floatingPointMode = false, precision = 38, scale = 0)
                else -> return null
            }
            DynamoDbAttributeGene(name, type, gene)
        }
        missingKeys.forEach { it.gene.randomize(randomness, false) }

        return DynamoDbAction(tableName, attributes + missingKeys)
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
