package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.gene.datetime.*
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.dynamodb.DynamoDbTemplateGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * Builds evolvable DynamoDB insertion actions from failed equality reads.
 */
object DynamoDbInsertBuilder {

    /**
     * Builds unique, supported DynamoDB insertion actions from failed equality reads.
     * Only absent primary-key values are randomized; inferred values retain their seeds.
     *
     * @param failedQueries failed reads reported by the controller
     * @param existingInsertionKeys keys of insertions that have already been added to the individual
     * @param randomness search randomness used to initialize missing keys
     * @return inferred actions not already represented by [existingInsertionKeys]
     */
    fun buildInsertActions(
        failedQueries: List<DynamoDbFailedQuery>,
        existingInsertionKeys: Set<String> = emptySet(),
        randomness: Randomness = Randomness()
    ): List<DynamoDbAction> = failedQueries
        .mapNotNull { toActionOrNull(it, randomness) }
        .filterNot { it.insertionKey() in existingInsertionKeys }
        .distinctBy { it.insertionKey() }

    /**
     * Builds insertion actions defaulting to no existing insertion keys.
     *
     * @param failedQueries failed reads reported by the controller
     * @param randomness search randomness used to initialize missing keys
     * @return inferred actions
     */
    fun buildInsertActions(
        failedQueries: List<DynamoDbFailedQuery>,
        randomness: Randomness
    ): List<DynamoDbAction> = buildInsertActions(failedQueries, emptySet(), randomness)

    /**
     * Converts one failed read into an insertion action, or returns null when it is incomplete.
     *
     * @param query failed read reported by the controller
     * @param randomness search randomness used to initialize missing keys
     * @return constructed insertion action, or null if incomplete
     */
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
                DynamoDbKeyScalarTypeDto.STRING -> if (query.primaryKeyFormats?.get(name) == "date-time" || isInstant(query.primaryKeyValueExamples?.get(name))) {
                    DateTimeGene(name, onlyValid = true, format = FormatForDatesAndTimes.RFC3339,
                        time = TimeGene("time", onlyValidTimes = true, format = FormatForDatesAndTimes.RFC3339,
                            offset = TimeOffsetGene("offset", ChoiceGene("type",
                                listOf(EnumGene("Z", listOf("Z"), treatAsNotString = true))))))
                } else StringGene(name, minLength = 1)
                // Seed whole numbers within DynamoDB's 38-digit precision limit.
                DynamoDbKeyScalarTypeDto.NUMBER -> BigDecimalGene(name, floatingPointMode = false, precision = 38, scale = 0)
                else -> return null
            }
            DynamoDbAttributeGene(name, type, gene)
        }
        missingKeys.forEach { it.gene.randomize(randomness, false) }

        return DynamoDbAction(tableName, attributes + missingKeys)
    }

    /**
     * Converts a supported scalar or composite DynamoDB attribute into its evolvable representation.
     *
     * @param attribute typed attribute value DTO
     * @return constructed attribute gene, or null if unsupported
     */
    private fun toAttributeOrNull(attribute: DynamoDbAttributeValueDto): DynamoDbAttributeGene? {
        val type = attribute.type ?: return null
        if (type == DynamoDbValueTypeDto.MAP || type == DynamoDbValueTypeDto.LIST || type == DynamoDbValueTypeDto.NULL) {
            return DynamoDbAttributeGene(attribute.attributeName, type, DynamoDbTemplateGene(attribute.attributeName, attribute))
        }
        val value = attribute.value ?: return null
        val gene = when (type) {
            DynamoDbValueTypeDto.STRING -> if (attribute.isTemplateValue)
                EnumGene(attribute.attributeName, listOf(value))
                else StringGene(attribute.attributeName, value)
            DynamoDbValueTypeDto.NUMBER -> value.toBigDecimalOrNull()?.let {
                BigDecimalGene(attribute.attributeName, it)
            } ?: return null
            DynamoDbValueTypeDto.BOOLEAN -> BooleanGene(attribute.attributeName, value.toBoolean())
            else -> return null
        }

        return DynamoDbAttributeGene(attribute.attributeName, type, gene)
    }

    /**
     * Infers timestamps from observed values rather than attribute names.
     *
     * @param value candidate string value
     * @return true if value matches an ISO-8601 instant
     */
    private fun isInstant(value: String?): Boolean = try {
        value != null && java.time.Instant.parse(value) != null
    } catch (_: java.time.format.DateTimeParseException) {
        false
    }
}
