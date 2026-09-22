package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFilterDto
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

    private fun hgetallCommand(key: String) = RedisFailedCommand().also {
        it.keys = listOf(key)
        it.command = "HGETALL"
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
        attributes: Map<String, String> = emptyMap(),
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
            attributes = mapOf("category" to "TAG", "title" to "TEXT"),
            filters = listOf(RedisSearchFilterDto.text(null, "redis"))
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertEquals("title", (actions[0] as RedisQueryAction).fields[0].name)
    }

    @Test
    fun testFtSearchFieldLessTextFilterWithoutAnyTextAttributeIsSkipped() {
        val cmd = ftCommand(
            attributes = mapOf("category" to "TAG"),
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
            attributes = mapOf("price" to "NUMERIC"),
            groupByFields = listOf("price")
        )
        val actions = RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())

        assertInstanceOf(DoubleGene::class.java, (actions[0] as RedisQueryAction).fields[0].valueGene)
    }
}