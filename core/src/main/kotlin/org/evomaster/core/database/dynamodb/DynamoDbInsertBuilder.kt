package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.HeterogeneousListGene
import org.evomaster.core.search.gene.collection.ProtectedUniqueArrayGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.service.Randomness
import java.math.BigDecimal
import java.util.TreeSet

/** Builds evolvable DynamoDB insertion actions from failed equality reads. */
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
                DynamoDbKeyScalarTypeDto.STRING -> StringGene(name, minLength = 1)
                // Seed whole numbers within DynamoDB's 38-digit precision limit.
                DynamoDbKeyScalarTypeDto.NUMBER -> BigDecimalGene(name, floatingPointMode = false, precision = 38, scale = 0)
                else -> return null
            }
            DynamoDbAttributeGene(name, DynamoDbValueType.fromKeyScalarDto(type), gene)
        }
        missingKeys.forEach { it.gene.randomize(randomness, false) }

        return DynamoDbAction(tableName, attributes + missingKeys)
    }

    /** Converts a supported scalar, set, or composite DynamoDB attribute into its evolvable representation. */
    private fun toAttributeOrNull(attribute: DynamoDbAttributeValueDto): DynamoDbAttributeGene? {
        val attrName = attribute.attributeName
        if (attrName.isNullOrBlank()) return null
        val dtoType = attribute.type ?: return null
        val type = DynamoDbValueType.fromDto(dtoType)
        if (type == DynamoDbValueType.MAP ||
            type == DynamoDbValueType.LIST ||
            type == DynamoDbValueType.STRING_SET ||
            type == DynamoDbValueType.NUMBER_SET ||
            type == DynamoDbValueType.NULL
        ) {
            val gene = try {
                buildDocumentGene(attrName, attribute)
            } catch (_: IllegalArgumentException) {
                return null
            }
            return DynamoDbAttributeGene(attrName, type, gene)
        }
        val value = attribute.value ?: return null
        val gene = when (type) {
            DynamoDbValueType.STRING -> StringGene(attrName, value)
            DynamoDbValueType.NUMBER -> value.toBigDecimalOrNull()?.let {
                BigDecimalGene(attrName, it)
            } ?: return null
            DynamoDbValueType.BOOLEAN -> BooleanGene(attrName, value.toBoolean())
            else -> return null
        }

        return DynamoDbAttributeGene(attrName, type, gene)
    }

    /**
     * Builds an EvoMaster gene tree from a composite, set, or null DynamoDB attribute DTO.
     * Maps use fixed-key [ObjectGene]s, lists (including empty `[]`) use mutable-size [HeterogeneousListGene]s
     * protecting inferred seed elements via `minSize`, string/number sets use [ProtectedUniqueArrayGene]s
     * preventing empty sets and duplicates, and nulls use [ImmutableDataHolderGene].
     */
    fun buildDocumentGene(name: String, dto: DynamoDbAttributeValueDto): Gene {
        fun build(currentName: String, currentDto: DynamoDbAttributeValueDto): Gene {
            return when (currentDto.type) {
                DynamoDbValueTypeDto.MAP -> {
                    val fields = currentDto.mapValue?.entries
                        ?.sortedBy { it.key }
                        ?.map { (key, childDto) -> build(key, childDto) }
                        ?: emptyList()
                    ObjectGene(currentName, fields)
                }
                DynamoDbValueTypeDto.LIST -> {
                    val elements = currentDto.listValue?.mapIndexed { index, childDto ->
                        build("${currentName}_$index", childDto)
                    } ?: emptyList()
                    val listTemplate = ChoiceGene(
                        "${currentName}_template",
                        listOf(
                            StringGene("${currentName}_s"),
                            BigDecimalGene("${currentName}_n"),
                            BooleanGene("${currentName}_b")
                        )
                    )
                    HeterogeneousListGene(
                        name = currentName,
                        elements = elements,
                        isFixed = false,
                        template = listTemplate,
                        minSize = elements.size,
                        maxSize = elements.size + ArrayGene.MAX_SIZE
                    )
                }
                DynamoDbValueTypeDto.STRING_SET -> {
                    val values = currentDto.setValue
                    if (values.isNullOrEmpty()) {
                        throw IllegalArgumentException("DynamoDB string sets cannot be empty")
                    }
                    val elements = values.distinct().mapIndexed { index, element ->
                        StringGene("${currentName}_$index", element)
                    }.toMutableList()
                    ProtectedUniqueArrayGene(
                        name = currentName,
                        template = StringGene("${currentName}_elem"),
                        protectedCount = elements.size,
                        minSize = 1,
                        maxSize = maxOf(ArrayGene.MAX_SIZE, elements.size + ArrayGene.MAX_SIZE),
                        elements = elements
                    )
                }
                DynamoDbValueTypeDto.NUMBER_SET -> {
                    val values = currentDto.setValue
                    if (values.isNullOrEmpty()) {
                        throw IllegalArgumentException("DynamoDB number sets cannot be empty")
                    }
                    val uniqueNumbers = TreeSet<BigDecimal>()
                    values.forEach { element ->
                        val num = element.ifBlank { "0" }.toBigDecimalOrNull()
                            ?: throw IllegalArgumentException("Invalid DynamoDB number set value: $element")
                        uniqueNumbers.add(num)
                    }
                    val elements = uniqueNumbers.mapIndexed { index, num ->
                        BigDecimalGene("${currentName}_$index", num)
                    }.toMutableList()
                    ProtectedUniqueArrayGene(
                        name = currentName,
                        template = BigDecimalGene("${currentName}_elem"),
                        protectedCount = elements.size,
                        minSize = 1,
                        maxSize = maxOf(ArrayGene.MAX_SIZE, elements.size + ArrayGene.MAX_SIZE),
                        elements = elements
                    )
                }
                DynamoDbValueTypeDto.NULL, null -> {
                    ImmutableDataHolderGene(currentName, "null", inQuotes = false)
                }
                DynamoDbValueTypeDto.STRING -> {
                    StringGene(currentName, currentDto.value ?: "")
                }
                DynamoDbValueTypeDto.NUMBER -> {
                    val num = currentDto.value?.ifBlank { "0" }?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    BigDecimalGene(currentName, num)
                }
                DynamoDbValueTypeDto.BOOLEAN -> {
                    BooleanGene(currentName, currentDto.value?.toBoolean() ?: false)
                }
            }
        }

        return build(name, dto)
    }
}
