package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.HeterogeneousListGene
import org.evomaster.core.search.gene.collection.ProtectedUniqueArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.datetime.TimeOffsetGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.service.Randomness
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.TreeSet
import java.util.UUID

/** Builds evolvable DynamoDB insertion actions from failed equality reads. */
object DynamoDbInsertBuilder {

    /** Maximum number of significant digits supported by DynamoDB's Number (`N`) data type. */
    private const val DYNAMODB_MAX_NUMBER_PRECISION = 38

    private const val DATE_GENE_NAME = "date"
    private const val YEAR_GENE_NAME = "year"
    private const val MONTH_GENE_NAME = "month"
    private const val DAY_GENE_NAME = "day"
    private const val TIME_GENE_NAME = "time"
    private const val HOUR_GENE_NAME = "hour"
    private const val MINUTE_GENE_NAME = "minute"
    private const val SECOND_GENE_NAME = "second"
    private const val MILLISECOND_GENE_NAME = "millisecond"
    private const val OFFSET_GENE_NAME = "offset"
    private const val OFFSET_TYPE_GENE_NAME = "type"
    private const val UTC_OFFSET_Z = "Z"
    private const val MOST_SIG_BITS_GENE_NAME = "mostSigBits"
    private const val LEAST_SIG_BITS_GENE_NAME = "leastSigBits"

    private const val MIN_MONTH = 1
    private const val MAX_MONTH = 12
    private const val MIN_DAY = 1
    private const val MAX_DAY = 31
    private const val MIN_HOUR = 0
    private const val MAX_HOUR = 23
    private const val MIN_MINUTE = 0
    private const val MAX_MINUTE = 59
    private const val MIN_SECOND = 0
    private const val MAX_SECOND = 59
    private const val MIN_MILLISECOND = 0
    private const val MAX_MILLISECOND = 999
    private const val NANOS_PER_MILLISECOND = 1_000_000

    private val DEFAULT_UTC_DATE_TIME = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    private val DEFAULT_LOCAL_DATE_TIME = LocalDateTime.of(2020, 1, 1, 0, 0, 0)
    private val DEFAULT_LOCAL_DATE = LocalDate.of(2020, 1, 1)
    private val DEFAULT_LOCAL_TIME = LocalTime.of(0, 0, 0)
    private val DEFAULT_UUID = UUID(0L, 0L)

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
     * TODO: Replace the hardcoded string format literals (`"date-time"`, `"local-date-time"`, `"date"`,
     *  `"local-time"`, `"uuid"`) with `DynamoDbAttributeFormat`. `core` does not depend on
     *  `evomaster-client-java-instrumentation`, so the only way to reuse that enum here is to move
     *  `DynamoDbAttributeFormat` to `evomaster-client-java-instrumentation-shared`.
     */
    private fun toActionOrNull(query: DynamoDbFailedQuery, randomness: Randomness): DynamoDbAction? {
        val tableName = query.tableName
        val queryAttributes = query.attributes
        if (tableName.isNullOrBlank() || queryAttributes.isNullOrEmpty()) return null

        val formats = query.primaryKeyFormats
        val attributes = queryAttributes.map { attribute ->
            toAttributeOrNull(attribute, formats) ?: return null
        }

        val missingKeys = (query.missingPrimaryKeyAttributes ?: emptyMap()).map { (name, type) ->
            if (name.isNullOrBlank() || attributes.any { it.attributeName == name }) return null
            val format = formats?.get(name)
            val gene = when (type) {
                DynamoDbKeyScalarTypeDto.STRING -> when {
                    format == "date-time" -> buildRfc3339DateTimeGene(name)
                    format == "local-date-time" -> buildIsoLocalDateTimeGene(name)
                    format == "date" -> buildDateGene(name)
                    format == "local-time" -> buildTimeGene(name)
                    format == "uuid" -> buildUuidGene(name)
                    isInstant(query.primaryKeyValueExamples?.get(name)) -> buildRfc3339DateTimeGene(name)
                    else -> StringGene(name, minLength = 1)
                }
                DynamoDbKeyScalarTypeDto.NUMBER -> buildNumberGene(name, initialValue = null, format = format, defaultWholeNumber = true)
                else -> return null
            }
            DynamoDbAttributeGene(name, DynamoDbValueType.fromKeyScalarDto(type), gene)
        }
        missingKeys.forEach { it.gene.randomize(randomness, false) }

        return DynamoDbAction(tableName, attributes + missingKeys)
    }

    /** Converts a supported scalar, set, or composite DynamoDB attribute into its evolvable representation. */
    private fun toAttributeOrNull(
        attribute: DynamoDbAttributeValueDto,
        formats: Map<String, String>?
    ): DynamoDbAttributeGene? {
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
        val format = formats?.get(attrName)
        val gene = when (type) {
            DynamoDbValueType.STRING -> buildStringOrFormattedGene(attrName, value, format)
            DynamoDbValueType.NUMBER -> value.toBigDecimalOrNull()?.let {
                buildNumberGene(attrName, initialValue = it, format = format, defaultWholeNumber = false)
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

    /**
     * Builds a formatted gene or fallback [StringGene] for a DynamoDB string attribute.
     *
     * TODO: Replace the hardcoded string format literals (`"date-time"`, `"local-date-time"`, `"date"`,
     *  `"local-time"`, `"uuid"`) with `DynamoDbAttributeFormat`. `core` does not depend on
     *  `evomaster-client-java-instrumentation`, so the only way to reuse that enum here is to move
     *  `DynamoDbAttributeFormat` to `evomaster-client-java-instrumentation-shared`.
     */
    private fun buildStringOrFormattedGene(name: String, value: String, format: String?): Gene =
        when (format) {
            "date-time" -> toDateTimeGeneOrNull(name, value, coerceIfDateTimeFormat = true)!!
            "local-date-time" -> buildIsoLocalDateTimeGene(name, value)
            "date" -> buildDateGene(name, value)
            "local-time" -> buildTimeGene(name, value)
            "uuid" -> buildUuidGene(name, value)
            else -> if (isInstant(value)) {
                toDateTimeGeneOrNull(name, value, coerceIfDateTimeFormat = false) ?: StringGene(name, value)
            } else {
                StringGene(name, value)
            }
        }

    private fun buildNumberGene(
        name: String,
        initialValue: BigDecimal?,
        format: String?,
        defaultWholeNumber: Boolean
    ): BigDecimalGene {
        val bounds = integerBoundsForFormat(format)
        if (bounds != null && (initialValue == null || initialValue.stripTrailingZeros().scale() <= 0)) {
            val (minBound, maxBound) = bounds
            val fitsBounds = initialValue == null ||
                ((minBound == null || initialValue >= minBound) && (maxBound == null || initialValue <= maxBound))
            val effectiveMin = if (fitsBounds) minBound else null
            val effectiveMax = if (fitsBounds) maxBound else null
            return BigDecimalGene(
                name = name,
                value = initialValue,
                min = effectiveMin,
                max = effectiveMax,
                floatingPointMode = false,
                precision = DYNAMODB_MAX_NUMBER_PRECISION,
                scale = 0
            )
        }
        return if (defaultWholeNumber) {
            BigDecimalGene(
                name = name,
                value = initialValue,
                floatingPointMode = false,
                precision = DYNAMODB_MAX_NUMBER_PRECISION,
                scale = 0
            )
        } else {
            BigDecimalGene(name, initialValue)
        }
    }

    /**
     * Returns `(min, max)` bounds for integer attribute formats, or `null` when unknown/unbounded.
     *
     * TODO: Replace the hardcoded integer format literals (`"int8"`, `"int16"`, `"int32"`, `"int64"`,
     *  `"integer"`) with `DynamoDbAttributeFormat`. `core` does not depend on
     *  `evomaster-client-java-instrumentation`, so the only way to reuse that enum here is to move
     *  `DynamoDbAttributeFormat` to `evomaster-client-java-instrumentation-shared`.
     */
    private fun integerBoundsForFormat(format: String?): Pair<BigDecimal?, BigDecimal?>? = when (format) {
        "int8" -> BigDecimal.valueOf(Byte.MIN_VALUE.toLong()) to BigDecimal.valueOf(Byte.MAX_VALUE.toLong())
        "int16" -> BigDecimal.valueOf(Short.MIN_VALUE.toLong()) to BigDecimal.valueOf(Short.MAX_VALUE.toLong())
        "int32" -> BigDecimal.valueOf(Int.MIN_VALUE.toLong()) to BigDecimal.valueOf(Int.MAX_VALUE.toLong())
        "int64" -> BigDecimal.valueOf(Long.MIN_VALUE) to BigDecimal.valueOf(Long.MAX_VALUE)
        "integer" -> null to null
        else -> null
    }

    private fun toDateTimeGeneOrNull(name: String, value: String, coerceIfDateTimeFormat: Boolean = false): DateTimeGene? {
        val instant = parseInstantOrNull(value)
            ?: if (coerceIfDateTimeFormat) parseCoercedInstantOrNull(value) else null
        if (instant == null) {
            return if (coerceIfDateTimeFormat) buildRfc3339DateTimeGene(name) else null
        }
        val utc = instant.atOffset(ZoneOffset.UTC)
        if (utc.year !in DateGene.MIN_YEAR..DateGene.MAX_YEAR) {
            return if (coerceIfDateTimeFormat) buildRfc3339DateTimeGene(name) else null
        }
        return buildRfc3339DateTimeGene(name, utc)
    }

    private fun parseInstantOrNull(value: String): Instant? =
        try { Instant.parse(value) } catch (_: DateTimeParseException) {
            try { OffsetDateTime.parse(value).toInstant() } catch (_: DateTimeParseException) {
                try { ZonedDateTime.parse(value).toInstant() } catch (_: DateTimeParseException) { null }
            }
        }

    private fun parseCoercedInstantOrNull(value: String): Instant? =
        try { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) } catch (_: DateTimeParseException) {
            try { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant() } catch (_: DateTimeParseException) { null }
        }

    private fun buildRfc3339DateTimeGene(name: String, utc: OffsetDateTime = DEFAULT_UTC_DATE_TIME): DateTimeGene {
        val millis = utc.nano / NANOS_PER_MILLISECOND
        return DateTimeGene(
            name,
            onlyValid = true,
            format = FormatForDatesAndTimes.RFC3339,
            date = DateGene(
                DATE_GENE_NAME,
                year = IntegerGene(YEAR_GENE_NAME, utc.year, DateGene.MIN_YEAR, DateGene.MAX_YEAR),
                month = IntegerGene(MONTH_GENE_NAME, utc.monthValue, MIN_MONTH, MAX_MONTH),
                day = IntegerGene(DAY_GENE_NAME, utc.dayOfMonth, MIN_DAY, MAX_DAY),
                onlyValidDates = true,
                format = FormatForDatesAndTimes.RFC3339
            ),
            time = TimeGene(
                TIME_GENE_NAME,
                hour = IntegerGene(HOUR_GENE_NAME, utc.hour, MIN_HOUR, MAX_HOUR),
                minute = IntegerGene(MINUTE_GENE_NAME, utc.minute, MIN_MINUTE, MAX_MINUTE),
                second = IntegerGene(SECOND_GENE_NAME, utc.second, MIN_SECOND, MAX_SECOND),
                format = FormatForDatesAndTimes.RFC3339,
                onlyValidTimes = true,
                millisecond = OptionalGene(
                    MILLISECOND_GENE_NAME,
                    IntegerGene(MILLISECOND_GENE_NAME, millis, MIN_MILLISECOND, MAX_MILLISECOND),
                    isActive = millis > 0
                ),
                offset = TimeOffsetGene(
                    OFFSET_GENE_NAME,
                    ChoiceGene(OFFSET_TYPE_GENE_NAME, listOf(EnumGene(UTC_OFFSET_Z, listOf(UTC_OFFSET_Z), treatAsNotString = true)))
                )
            )
        )
    }

    private fun buildIsoLocalDateTimeGene(name: String, value: String? = null): DateTimeGene {
        val parsed = if (value == null) DEFAULT_LOCAL_DATE_TIME else try { LocalDateTime.parse(value) } catch (_: DateTimeParseException) {
            try { Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDateTime() } catch (_: DateTimeParseException) {
                try { OffsetDateTime.parse(value).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() } catch (_: DateTimeParseException) {
                    try { LocalDate.parse(value).atStartOfDay() } catch (_: DateTimeParseException) { DEFAULT_LOCAL_DATE_TIME }
                }
            }
        }
        val localDateTime = if (parsed.year in DateGene.MIN_YEAR..DateGene.MAX_YEAR) parsed else DEFAULT_LOCAL_DATE_TIME
        return DateTimeGene(
            name,
            onlyValid = true,
            format = FormatForDatesAndTimes.ISO_LOCAL,
            date = DateGene(
                DATE_GENE_NAME,
                year = IntegerGene(YEAR_GENE_NAME, localDateTime.year, DateGene.MIN_YEAR, DateGene.MAX_YEAR),
                month = IntegerGene(MONTH_GENE_NAME, localDateTime.monthValue, MIN_MONTH, MAX_MONTH),
                day = IntegerGene(DAY_GENE_NAME, localDateTime.dayOfMonth, MIN_DAY, MAX_DAY),
                onlyValidDates = true,
                format = FormatForDatesAndTimes.ISO_LOCAL
            ),
            time = TimeGene(
                TIME_GENE_NAME,
                hour = IntegerGene(HOUR_GENE_NAME, localDateTime.hour, MIN_HOUR, MAX_HOUR),
                minute = IntegerGene(MINUTE_GENE_NAME, localDateTime.minute, MIN_MINUTE, MAX_MINUTE),
                second = IntegerGene(SECOND_GENE_NAME, localDateTime.second, MIN_SECOND, MAX_SECOND),
                format = FormatForDatesAndTimes.ISO_LOCAL,
                onlyValidTimes = true
            )
        )
    }

    private fun buildDateGene(name: String, value: String? = null): DateGene {
        val parsed = if (value == null) DEFAULT_LOCAL_DATE else try { LocalDate.parse(value) } catch (_: DateTimeParseException) { DEFAULT_LOCAL_DATE }
        val localDate = if (parsed.year in DateGene.MIN_YEAR..DateGene.MAX_YEAR) parsed else DEFAULT_LOCAL_DATE
        return DateGene(
            name,
            year = IntegerGene(YEAR_GENE_NAME, localDate.year, DateGene.MIN_YEAR, DateGene.MAX_YEAR),
            month = IntegerGene(MONTH_GENE_NAME, localDate.monthValue, MIN_MONTH, MAX_MONTH),
            day = IntegerGene(DAY_GENE_NAME, localDate.dayOfMonth, MIN_DAY, MAX_DAY),
            onlyValidDates = true,
            format = FormatForDatesAndTimes.ISO_LOCAL
        )
    }

    private fun buildTimeGene(name: String, value: String? = null): TimeGene {
        val localTime = if (value == null) DEFAULT_LOCAL_TIME else try { LocalTime.parse(value) } catch (_: DateTimeParseException) { DEFAULT_LOCAL_TIME }
        return TimeGene(
            name,
            hour = IntegerGene(HOUR_GENE_NAME, localTime.hour, MIN_HOUR, MAX_HOUR),
            minute = IntegerGene(MINUTE_GENE_NAME, localTime.minute, MIN_MINUTE, MAX_MINUTE),
            second = IntegerGene(SECOND_GENE_NAME, localTime.second, MIN_SECOND, MAX_SECOND),
            format = FormatForDatesAndTimes.ISO_LOCAL,
            onlyValidTimes = true
        )
    }

    private fun buildUuidGene(name: String, value: String? = null): UUIDGene {
        val uuid = if (value == null) DEFAULT_UUID else try { UUID.fromString(value) } catch (_: IllegalArgumentException) { DEFAULT_UUID }
        return UUIDGene(
            name,
            mostSigBits = LongGene(MOST_SIG_BITS_GENE_NAME, uuid.mostSignificantBits),
            leastSigBits = LongGene(LEAST_SIG_BITS_GENE_NAME, uuid.leastSignificantBits)
        )
    }

    private fun isInstant(value: String?): Boolean = try {
        value != null && Instant.parse(value) != null
    } catch (_: DateTimeParseException) { false }
}
