package org.evomaster.core.output

import org.evomaster.core.redis.*
import org.evomaster.core.search.action.EvaluatedRedisDbAction
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RedisWriterTest {

    private fun makeEvaluatedSet(key: String, value: String, success: Boolean = true): EvaluatedRedisDbAction {
        val action = RedisSetAction(key = key, valueGene = StringGene("value", value))
        action.setLocalId("test-redis-set-action")
        val result = RedisDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun makeEvaluatedHset(key: String, field: String, value: String, success: Boolean = true): EvaluatedRedisDbAction {
        val action = RedisHsetAction(key = key, field = field, valueGene = StringGene("value", value))
        action.setLocalId("test-redis-hset-action")
        val result = RedisDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun makeEvaluatedSadd(key: String, member: String, success: Boolean = true): EvaluatedRedisDbAction {
        val action = RedisSaddAction(key = key, memberGene = StringGene("member", member))
        action.setLocalId("test-redis-sadd-action")
        val result = RedisDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun makeEvaluatedSaddFromSinter(keys: List<String>, member: String, success: Boolean = true): EvaluatedRedisDbAction {
        val action = RedisSaddFromSinterAction(keys = keys, memberGene = StringGene("member", member))
        action.setLocalId("test-redis-sadd-sinter-action")
        val result = RedisDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun writeKotlin(
        actions: List<EvaluatedRedisDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false,
        groupIndex: String = ""
    ): String {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)
        RedisWriter.handleRedisDbInitialization(OutputFormat.KOTLIN_JUNIT_5, actions, lines, groupIndex, insertionVars, skipFailure)
        return lines.toString()
    }

    private fun writeJava(
        actions: List<EvaluatedRedisDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false
    ): String {
        val lines = Lines(OutputFormat.JAVA_JUNIT_5)
        RedisWriter.handleRedisDbInitialization(OutputFormat.JAVA_JUNIT_5, actions, lines, "", insertionVars, skipFailure)
        return lines.toString()
    }

    @Test
    fun testEmptyListGeneratesNothing() {
        assertTrue(writeKotlin(emptyList()).isBlank())
    }

    @Test
    fun testAllFailedWithSkipFailureGeneratesNothing() {
        val output = writeKotlin(listOf(makeEvaluatedSet("k", "v", success = false)), skipFailure = true)
        assertTrue(output.isBlank())
    }

    @Test
    fun testKotlinFormatSingleSetAction() {
        val output = writeKotlin(listOf(makeEvaluatedSet("user:1", "Alice")))
        assertTrue(output.contains("val insertions_redis = redis()"))
        assertTrue(output.contains(""".set("user:1", "Alice")"""))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testKotlinFormatSingleHsetAction() {
        val output = writeKotlin(listOf(makeEvaluatedHset("user:1", "name", "Alice")))
        assertTrue(output.contains(""".hset("user:1", "name", "Alice")"""))
    }

    @Test
    fun testJavaFormatSingleHsetAction() {
        val output = writeJava(listOf(makeEvaluatedHset("user:1", "name", "Alice")))
        assertTrue(output.contains("List<RedisInsertionDto> insertions_redis = redis()"))
        assertTrue(output.contains(""".hset("user:1", "name", "Alice")"""))
        assertTrue(output.contains("RedisInsertionResultsDto insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testKotlinFormatSingleSaddAction() {
        val output = writeKotlin(listOf(makeEvaluatedSadd("myset", "item1")))
        assertTrue(output.contains(""".sadd("myset", "item1")"""))
    }

    @Test
    fun testSaddFromSinterGeneratesOneSaddPerKey() {
        val output = writeKotlin(listOf(makeEvaluatedSaddFromSinter(listOf("set1", "set2", "set3"), "shared")))

        assertTrue(output.contains(""".sadd("set1", "shared")"""))
        assertTrue(output.contains(""".sadd("set2", "shared")"""))
        assertTrue(output.contains(""".sadd("set3", "shared")"""))
        assertEquals(2, output.split(".and()").size - 1)
    }

    @Test
    fun testSkipFailureOmitsFailedActions() {
        val actions = listOf(
            makeEvaluatedSet("k1", "v1", success = true),
            makeEvaluatedSet("k2", "v2", success = false)
        )
        val output = writeKotlin(actions, skipFailure = true)
        assertTrue(output.contains("k1"))
        assertFalse(output.contains("k2"))
    }

    @Test
    fun testMultipleActionsUsesAndChaining() {
        val actions = listOf(makeEvaluatedSet("k1", "v1"), makeEvaluatedSet("k2", "v2"))
        val output = writeKotlin(actions)
        assertTrue(output.contains(".and()"))
    }

    @Test
    fun testMixedActions() {
        val actions = listOf(
            makeEvaluatedSet("string:key", "val"),
            makeEvaluatedHset("hash:key", "field1", "val2"),
            makeEvaluatedSadd("set:key", "member"),
            makeEvaluatedSaddFromSinter(listOf("s1", "s2"), "shared")
        )
        val output = writeKotlin(actions)
        assertTrue(output.contains(""".set("string:key", "val")"""))
        assertTrue(output.contains(""".hset("hash:key", "field1", "val2")"""))
        assertTrue(output.contains(""".sadd("set:key", "member")"""))
        assertTrue(output.contains(""".sadd("s1", "shared")"""))
        assertTrue(output.contains(""".sadd("s2", "shared")"""))
    }

    @Test
    fun testKotlinEscapesDollarSign() {
        val output = writeKotlin(listOf(makeEvaluatedSet("key", "\$HOME")))
        assertTrue(output.contains("\\${'$'}HOME") || output.contains("\\\$HOME"))
        assertFalse(output.contains("\"\$HOME\""))
    }

    @Test
    fun testKeyWithSpecialCharactersIsEscaped() {
        val output = writeKotlin(listOf(makeEvaluatedSet("key\"with\"quotes", "value")))
        assertTrue(output.contains("key\\\"with\\\"quotes"))
    }

    @Test
    fun testGroupIndexAppearsInVariableName() {
        val output = writeKotlin(listOf(makeEvaluatedSet("key:1", "val")), groupIndex = "_42")
        assertTrue(output.contains("insertions_redis_42"))
    }

    @Test
    fun testInsertionVarsAccumulated() {
        val insertionVars = mutableListOf<Pair<String, String>>()
        writeKotlin(listOf(makeEvaluatedSet("key:1", "val")), insertionVars = insertionVars)

        assertEquals(1, insertionVars.size)
        assertEquals("insertions_redis", insertionVars[0].first)
        assertEquals("insertions_redis_result", insertionVars[0].second)
    }
}