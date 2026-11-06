package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFieldType
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFilterDto
import org.evomaster.client.java.instrumentation.`object`.ClassToSchema
import org.evomaster.client.java.instrumentation.`object`.RedisTypeToOasConverter
import org.evomaster.core.database.redis.RedisDbActionTransformer
import org.evomaster.core.database.redis.RedisHsetAction
import org.evomaster.core.database.redis.RedisInsertBuilder
import org.evomaster.core.database.redis.RedisQueryAction
import org.evomaster.core.database.redis.RedisSaddAction
import org.evomaster.core.database.redis.RedisSaddFromSinterAction
import org.evomaster.core.database.redis.RedisSetAction
import org.evomaster.core.database.redis.RedisSetFromPatternAction
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RedisInsertBuilderTest {

    // --- GET ---

    @Test
    fun testGetBuildsSetActionForEachCommand() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))
        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet())

        assertEquals(2, actions.size)
        actions.forEach { assertTrue(it is RedisSetAction) }
        assertEquals("user:1", (actions[0] as RedisSetAction).key)
        assertEquals("user:2", (actions[1] as RedisSetAction).key)
    }

    @Test
    fun testGetSkipsExistingKeys() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))
        val actions = RedisInsertBuilder.buildInsertActions(commands, setOf("user:1"))

        assertEquals(1, actions.size)
        assertEquals("user:2", (actions[0] as RedisSetAction).key)
    }

    @Test
    fun testGetKeyInitializedWithObservedKey() {
        val actions = RedisInsertBuilder.buildInsertActions(listOf(getCommand("known:key")), emptySet())
        assertEquals("known:key", (actions[0] as RedisSetAction).key)
    }

    // --- HGET ---

    @Test
    fun testHgetBuildsHsetAction() {
        val actions = RedisInsertBuilder.buildInsertActions(listOf(hgetCommand("user:1", "name")), emptySet())

        assertEquals(1, actions.size)
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.key)
        assertEquals("name", action.field)
    }

    // --- HGETALL ---

    @Test
    fun testHgetallBuildsHsetActionWithPlaceholderField() {
        val actions = RedisInsertBuilder.buildInsertActions(listOf(hgetallCommand("user:1")), emptySet())

        assertEquals(1, actions.size)
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.key)
        assertEquals("field", action.field)
    }

    @Test
    fun testHgetallWithEntityTypeBuildsQueryActionWithOnePerProperty() {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisHashTestEntity::class.java)
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("entities:1", entityType = schema)), emptySet()
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisQueryAction
        assertEquals("entities:1", action.key)
        assertEquals(setOf("id", "name", "age", "address"), action.fields.map { it.name }.toSet())
    }

    @Test
    fun testHgetallInactiveOptionalFieldIsLeftOutOfTheHash() {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisHashTestEntity::class.java)
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("entities:1", entityType = schema)), emptySet()
        )
        val action = actions[0] as RedisQueryAction

        val allFields = RedisDbActionTransformer.transform(actions).insertions.map { it.field }.toSet()
        assertEquals(setOf("id", "name", "age", "address"), allFields)

        (action.fields.first { it.name == "address" }.valueGene as OptionalGene).isActive = false
        (action.fields.first { it.name == "age" }.valueGene as OptionalGene).isActive = false

        val remaining = RedisDbActionTransformer.transform(actions).insertions.map { it.field }.toSet()
        assertEquals(setOf("id", "name"), remaining)
        assertEquals(remaining.size, action.insertionsCount())
    }

    @Test
    fun testHgetallNestedObjectsAreFlattenedIntoDottedFields() {
        val hash = hashFor(RedisHashRichEntity::class.java)

        // two properties of the same nested type must each get their own prefix
        assertTrue(hash.keys.containsAll(listOf("address.city", "address.zip", "work.city", "work.zip")), hash.keys.toString())
        // a nested object is never written as a single field holding its JSON
        assertFalse(hash.containsKey("address"))
        assertFalse(hash.containsKey("work"))
    }

    @Test
    fun testHgetallLeavesOfEveryBasicTypeAreWrittenInTheFormatSpringReadsBack() {
        val hash = hashFor(RedisHashRichEntity::class.java)

        // the parsers below are the ones Spring Data Redis uses to read each of these types
        java.time.LocalDate.parse(hash.getValue("localDate"))
        java.time.LocalDateTime.parse(hash.getValue("localDateTime"))
        java.time.LocalTime.parse(hash.getValue("localTime"))
        java.time.Instant.parse(hash.getValue("instant"))
        java.util.UUID.fromString(hash.getValue("uuid"))
        java.math.BigDecimal(hash.getValue("price"))
        java.math.BigInteger(hash.getValue("big"))
        // java.util.Date is stored as epoch milliseconds, not as a formatted date
        assertTrue(hash.getValue("date").toLong() >= 0, hash.getValue("date"))
        RedisHashRichEntity.Color.valueOf(hash.getValue("color"))
        assertTrue(hash.getValue("flag") in listOf("true", "false"))
        hash.getValue("l").toLong()
        hash.getValue("d").toDouble()
        hash.getValue("f").toFloat()
        hash.getValue("s").toShort()
        hash.getValue("b").toByte()
        assertEquals(1, hash.getValue("c").length)
    }

    @Test
    fun testHgetallEmptyCollectionsAndMapsWriteNothing() {
        val hash = hashFor(RedisHashRichEntity::class.java)

        // they start empty, and an empty collection is stored as no field at all
        assertTrue(hash.keys.none { it.startsWith("tags") || it.startsWith("addrs") || it.startsWith("attrs") }, hash.keys.toString())
    }

    @Test
    fun testHgetallIdFieldIsFixedToTheIdOfTheKey() {
        val hash = hashFor(RedisHashRichEntity::class.java, key = "rich:42", idField = "id")

        assertEquals("42", hash["id"])
    }

    @Test
    fun testHgetallIdFieldIsNotAFreeGene() {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisHashRichEntity::class.java, false, RedisTypeToOasConverter.defaults())
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("rich:42", entityType = schema, idField = "id")), emptySet()
        )
        val idField = (actions[0] as RedisQueryAction).fields.first { it.name == "id" }

        assertEquals("42", idField.constantValue)
        assertNull(idField.valueGene)
    }

    @Test
    fun testHgetallIdFieldMissingFromTheSchemaIsIgnored() {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisHashRichEntity::class.java, false, RedisTypeToOasConverter.defaults())
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("rich:42", entityType = schema, idField = "notAProperty")), emptySet()
        )

        // nothing matches, so nothing is fixed
        assertTrue((actions[0] as RedisQueryAction).fields.all { it.valueGene != null })
    }

    @Test
    fun testHgetallWithoutIdFieldKeepsEveryFieldFree() {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(RedisHashRichEntity::class.java, false, RedisTypeToOasConverter.defaults())
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("rich:42", entityType = schema)), emptySet()
        )

        assertTrue((actions[0] as RedisQueryAction).fields.all { it.valueGene != null })
    }

    @Test
    fun testHgetallWithUnparsableEntityTypeFallsBackToPlaceholderField() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("user:1", entityType = "not a valid schema")), emptySet()
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisHsetAction
        assertEquals("field", action.field)
    }

    // --- KEYS ---

    @Test
    fun testKeysBuildsSetFromPatternAction() {
        val actions = RedisInsertBuilder.buildInsertActions(listOf(keysCommand("^user:.*$")), emptySet())

        assertEquals(1, actions.size)
        assertInstanceOf(RedisSetFromPatternAction::class.java, actions[0])
        assertEquals("^user:.*$", (actions[0] as RedisSetFromPatternAction).keyGene.sourceRegex)
    }

    // --- SMEMBERS ---

    @Test
    fun testSmembersBuildsRedisSaddAction() {
        val actions = RedisInsertBuilder.buildInsertActions(listOf(smembersCommand("myset")), emptySet())

        assertEquals(1, actions.size)
        assertEquals("myset", (actions[0] as RedisSaddAction).key)
    }

    @Test
    fun testSmembersSkipsExistingKey() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(smembersCommand("myset")), setOf("myset")
        )
        assertTrue(actions.isEmpty())
    }

    // --- SINTER ---

    @Test
    fun testSinterBuildsRedisSaddFromSinterAction() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(sinterCommand("set1", "set2", "set3")), emptySet()
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisSaddFromSinterAction
        assertEquals(listOf("set1", "set2", "set3"), action.keys)
    }

    @Test
    fun testSinterAlwaysProcessedEvenIfKeysExist() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(sinterCommand("set1", "set2")), setOf("set1", "set2")
        )
        assertEquals(1, actions.size)
        assertInstanceOf(RedisSaddFromSinterAction::class.java, actions[0])
    }

    @Test
    fun testSinterSharesSingleMemberGeneAcrossKeys() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(sinterCommand("set1", "set2")), emptySet()
        )
        val action = actions[0] as RedisSaddFromSinterAction
        assertEquals(2, action.keys.size)
        assertNotNull(action.memberGene)
    }

    // --- general ---

    @Test
    fun testSkipsAllIfAllExist() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))
        val actions = RedisInsertBuilder.buildInsertActions(commands, setOf("user:1", "user:2"))
        assertTrue(actions.isEmpty())
    }

    @Test
    fun testEmptyCommandsReturnsEmptyList() {
        assertTrue(RedisInsertBuilder.buildInsertActions(emptyList(), emptySet()).isEmpty())
    }

    @Test
    fun testEachActionHasIndependentValueGene() {
        val commands = listOf(getCommand("key:1"), getCommand("key:2"))
        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet())

        (actions[0] as RedisSetAction).valueGene.value = "mutated"
        assertNotEquals("mutated", (actions[1] as RedisSetAction).valueGene.value)
    }

    @Test
    fun testUnsupportedCommandThrowsAssertionError() {
        val unsupported = RedisFailedCommand().also {
            it.keys = listOf("key:1")
            it.command = "UNKNOWN_CMD"
        }
        assertThrows<AssertionError> {
            RedisInsertBuilder.buildInsertActions(listOf(unsupported), emptySet())
        }
    }

    private fun getCommand(key: String) = RedisFailedCommand().also {
        it.keys = listOf(key)
        it.command = "GET"
    }

    private fun hgetCommand(key: String, field: String) = RedisFailedCommand().also {
        it.keys = listOf(key)
        it.field = field
        it.command = "HGET"
    }

    private fun hgetallCommand(key: String, entityType: String? = null, idField: String? = null) = RedisFailedCommand().also {
        it.keys = listOf(key)
        it.command = "HGETALL"
        it.entityType = entityType
        it.entityIdField = idField
    }

    /**
     * The hash that would be stored for [entity] under [key]: the field name -> value of every
     * HSET generated for it. The schema is derived the same way the replacement does for a real
     * `@RedisHash` class.
     */
    private fun hashFor(entity: Class<*>, key: String = "rich:1", idField: String? = null): Map<String, String> {
        val schema = ClassToSchema.getOrDeriveSchemaWithItsRef(entity, false, RedisTypeToOasConverter.defaults())
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand(key, entityType = schema, idField = idField)), emptySet()
        )
        return RedisDbActionTransformer.transform(actions).insertions.associate { it.field to it.value }
    }

    private fun keysCommand(pattern: String) = RedisFailedCommand().also {
        it.pattern = pattern
        it.command = "KEYS"
    }

    private fun smembersCommand(key: String) = RedisFailedCommand().also {
        it.keys = listOf(key)
        it.command = "SMEMBERS"
    }

    private fun sinterCommand(vararg keys: String) = RedisFailedCommand().also {
        it.keys = keys.toList()
        it.command = "SINTER"
    }

    // --- FT.SEARCH / FT.AGGREGATE ---

    private fun ftCommand(
        command: String = "FT_SEARCH",
        indexName: String = "idx:products",
        prefixes: List<String> = listOf("product:"),
        attributes: Map<String, RedisSearchFieldType> = emptyMap(),
        filters: List<RedisSearchFilterDto> = emptyList(),
        groupByFields: List<String> = emptyList()
    ) = RedisFailedCommand(command, indexName, prefixes, attributes, filters, groupByFields)

    @Test
    fun testFtSearchTagFilterBuildsEnumGeneField() {
        val cmd = ftCommand(filters = listOf(RedisSearchFilterDto.tag("street", listOf("main", "second"))))
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertEquals(1, actions.size)
        val action = actions[0] as RedisQueryAction
        assertEquals(1, action.fields.size)
        assertEquals("street", action.fields[0].name)
        assertInstanceOf(EnumGene::class.java, action.fields[0].valueGene)
    }

    @Test
    fun testFtSearchNumericFilterBuildsDoubleGeneFieldWithinBounds() {
        val cmd = ftCommand(filters = listOf(RedisSearchFilterDto.numeric("age", 18.0, 65.0)))
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        val gene = (actions[0] as RedisQueryAction).fields[0].valueGene as DoubleGene
        assertTrue(gene.value in 18.0..65.0)
    }

    @Test
    fun testFtSearchExactTextFilterIsAConstantNotAGene() {
        val cmd = ftCommand(filters = listOf(RedisSearchFilterDto.text("title", "redis")))
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        val field = (actions[0] as RedisQueryAction).fields[0]
        assertEquals("redis", field.constantValue)
        assertNull(field.valueGene)
    }

    @Test
    fun testFtSearchPrefixTextFilterBuildsRegexGeneField() {
        val cmd = ftCommand(filters = listOf(RedisSearchFilterDto.text("title", "ali*")))
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        val gene = (actions[0] as RedisQueryAction).fields[0].valueGene as RegexGene
        assertTrue(gene.getValueAsRawString().startsWith("ali"))
    }

    @Test
    fun testFtSearchFieldLessTextFilterUsesFirstTextAttribute() {
        val cmd = ftCommand(
            attributes = mapOf("category" to RedisSearchFieldType.TAG, "title" to RedisSearchFieldType.TEXT),
            filters = listOf(RedisSearchFilterDto.text(null, "redis"))
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertEquals("title", (actions[0] as RedisQueryAction).fields[0].name)
    }

    @Test
    fun testFtSearchFieldLessTextFilterWithoutAnyTextAttributeIsSkipped() {
        val cmd = ftCommand(
            attributes = mapOf("category" to RedisSearchFieldType.TAG),
            filters = listOf(RedisSearchFilterDto.text(null, "redis"))
        )

        assertTrue(RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet()).isEmpty())
    }

    @Test
    fun testFtSearchGeneratedKeyStartsWithIndexPrefix() {
        val cmd = ftCommand(
            prefixes = listOf("product:"),
            filters = listOf(RedisSearchFilterDto.text("title", "redis"))
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertTrue((actions[0] as RedisQueryAction).key.startsWith("product:"))
    }

    @Test
    fun testFtSearchWithoutDeclaredPrefixesIsSkipped() {
        val cmd = ftCommand(prefixes = emptyList(), filters = listOf(RedisSearchFilterDto.text("title", "redis")))
        assertTrue(RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet()).isEmpty())
    }

    @Test
    fun testFtSearchSameCommandReusesTheSameKeyInsteadOfDuplicating() {
        val cmd = ftCommand(filters = listOf(RedisSearchFilterDto.text("title", "redis")))
        val first = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())
        val key = (first[0] as RedisQueryAction).key

        val second = RedisInsertBuilder.buildInsertActions(listOf(cmd), setOf(key))
        assertTrue(second.isEmpty(), "a document already covering this exact command should not be duplicated")
    }

    @Test
    fun testFtSearchDifferentCommandsMapToDifferentKeys() {
        val redis = ftCommand(filters = listOf(RedisSearchFilterDto.text("title", "redis")))
        val mongo = ftCommand(filters = listOf(RedisSearchFilterDto.text("title", "mongodb")))

        val keyForRedis = (RedisInsertBuilder.buildInsertActions(listOf(redis), emptySet())[0] as RedisQueryAction).key
        val keyForMongo = (RedisInsertBuilder.buildInsertActions(listOf(mongo), emptySet())[0] as RedisQueryAction).key

        assertNotEquals(keyForRedis, keyForMongo)
    }

    @Test
    fun testFtSearchMatchAllWithoutGroupByUsesAPlaceholderField() {
        val cmd = ftCommand(filters = emptyList())
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        val action = actions[0] as RedisQueryAction
        assertEquals(1, action.fields.size)
        assertEquals("value", action.fields[0].name)
    }

    @Test
    fun testFtAggregateGroupByFieldWithoutFilterGetsAFreeGene() {
        val cmd = ftCommand(command = "FT_AGGREGATE", groupByFields = listOf("category"))
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        val action = actions[0] as RedisQueryAction
        assertEquals(1, action.fields.size)
        assertEquals("category", action.fields[0].name)
        assertNotNull(action.fields[0].valueGene)
    }

    @Test
    fun testFtAggregateGroupByFieldSharedWithFilterIsNotDuplicated() {
        val cmd = ftCommand(
            command = "FT_AGGREGATE",
            filters = listOf(RedisSearchFilterDto.tag("category", listOf("books"))),
            groupByFields = listOf("category")
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertEquals(1, (actions[0] as RedisQueryAction).fields.size)
    }

    @Test
    fun testFtAggregateNumericGroupByFieldUsesDoubleGene() {
        val cmd = ftCommand(
            command = "FT_AGGREGATE",
            attributes = mapOf("price" to RedisSearchFieldType.NUMERIC),
            groupByFields = listOf("price")
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertInstanceOf(DoubleGene::class.java, (actions[0] as RedisQueryAction).fields[0].valueGene)
    }

    @Test
    fun testFtAggregateGroupByFieldOfUnhandledTypeGetsAStringGene() {
        val cmd = ftCommand(
            command = "FT_AGGREGATE",
            attributes = mapOf("location" to RedisSearchFieldType.OTHER),
            groupByFields = listOf("location")
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertInstanceOf(StringGene::class.java, (actions[0] as RedisQueryAction).fields[0].valueGene)
    }

    @Test
    fun testFtSearchFieldLessTextFilterPicksTheFirstTextAttributeInDeclarationOrder() {
        val cmd = ftCommand(
            attributes = mapOf(
                "location" to RedisSearchFieldType.OTHER,
                "title" to RedisSearchFieldType.TEXT,
                "description" to RedisSearchFieldType.TEXT
            ),
            filters = listOf(RedisSearchFilterDto.text(null, "redis"))
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertEquals("title", (actions[0] as RedisQueryAction).fields[0].name)
    }
}

/**
 * Stands in for a `@RedisHash`-annotated class, as used by [RedisInsertBuilderTest] to derive a
 * schema the same way [org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.AbstractBasicRedisPersistentEntityClassReplacement]
 * would for a real one.
 */
class RedisHashTestEntity {
    var id: String? = null
    var name: String? = null
    var age: Int = 0
    var address: String? = null
}
class RedisHashTestAddress {
    var city: String? = null
    var zip: Int? = null
}

/**
 * Has one property of each basic type, two of the same nested type, and a collection, a list of
 * nested objects and a map.
 */
class RedisHashRichEntity {

    enum class Color { RED, BLUE }

    var id: String? = null
    var color: Color? = null
    var price: java.math.BigDecimal? = null
    var big: java.math.BigInteger? = null
    var flag: Boolean = false
    var l: Long = 0L
    var d: Double = 0.0
    var f: Float = 0f
    var s: Short = 0
    var b: Byte = 0
    var c: Char = 'a'
    var date: java.util.Date? = null
    var localDate: java.time.LocalDate? = null
    var localDateTime: java.time.LocalDateTime? = null
    var localTime: java.time.LocalTime? = null
    var instant: java.time.Instant? = null
    var uuid: java.util.UUID? = null
    var address: RedisHashTestAddress? = null
    var work: RedisHashTestAddress? = null
    var tags: List<String>? = null
    var addrs: List<RedisHashTestAddress>? = null
    var attrs: Map<String, String>? = null
}
