package org.evomaster.core.output

import org.evomaster.core.redis.RedisDbActionResult
import org.evomaster.core.redis.RedisHsetAction
import org.evomaster.core.redis.RedisSaddAction
import org.evomaster.core.redis.RedisSetAction
import org.evomaster.core.search.action.EvaluatedRedisDbAction
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RedisWriterTest {

    private fun makeEvaluatedSet(
        key: String,
        value: String,
        success: Boolean = true
    ): EvaluatedRedisDbAction {
        val action = RedisSetAction(
            keyGene = StringGene("key", key),
            valueGene = StringGene("value", value)
        )
        action.setLocalId("test-redis-action")
        val result = RedisDbActionResult(action.getLocalId()).also {
            it.setInsertExecutionResult(success)
        }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun makeEvaluatedHset(
        key: String,
        field: String,
        value: String,
        success: Boolean = true
    ): EvaluatedRedisDbAction {
        val action = RedisHsetAction(
            keyGene = StringGene("key", key),
            field = field,
            valueGene = StringGene("value", value)
        )
        action.setLocalId("test-redis-hset-action")
        val result = RedisDbActionResult(action.getLocalId()).also {
            it.setInsertExecutionResult(success)
        }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun makeEvaluatedSadd(
        key: String,
        member: String,
        success: Boolean = true
    ): EvaluatedRedisDbAction {
        val action = RedisSaddAction(
            keyGene = StringGene("key", key),
            memberGene = StringGene("value", member)
        )
        action.setLocalId("test-redis-sadd-action")
        val result = RedisDbActionResult(action.getLocalId()).also {
            it.setInsertExecutionResult(success)
        }
        return EvaluatedRedisDbAction(action, result)
    }

    private fun writeKotlin(
        actions: List<EvaluatedRedisDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false,
        groupIndex: String = ""
    ): String {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)
        RedisWriter.handleRedisDbInitialization(
            format = OutputFormat.KOTLIN_JUNIT_5,
            redisDbInitialization = actions,
            lines = lines,
            groupIndex = groupIndex,
            insertionVars = insertionVars,
            skipFailure = skipFailure
        )
        return lines.toString()
    }

    private fun writeJava(
        actions: List<EvaluatedRedisDbAction>,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false
    ): String {
        val lines = Lines(OutputFormat.JAVA_JUNIT_5)
        RedisWriter.handleRedisDbInitialization(
            format = OutputFormat.JAVA_JUNIT_5,
            redisDbInitialization = actions,
            lines = lines,
            insertionVars = insertionVars,
            skipFailure = skipFailure
        )
        return lines.toString()
    }

    @Test
    fun testEmptyListGeneratesNothing() {
        val output = writeKotlin(emptyList())
        assertTrue(output.isBlank())
    }

    @Test
    fun testKotlinFormatSingleHsetAction() {
        val output = writeKotlin(listOf(makeEvaluatedHset("user:1", "name", "Alice")))

        assertTrue(output.contains("val insertions_redis = redis()"))
        assertTrue(output.contains(".hset(\"user:1\", \"name\", \"Alice\")"))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testJavaFormatSingleHsetAction() {
        val output = writeJava(listOf(makeEvaluatedHset("user:1", "name", "Alice")))

        assertTrue(output.contains("List<RedisInsertionDto> insertions_redis = redis()"))
        assertTrue(output.contains(".hset(\"user:1\", \"name\", \"Alice\")"))
        assertTrue(output.contains("RedisInsertionResultsDto insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testKotlinFormatSingleSaddAction() {
        val output = writeKotlin(listOf(makeEvaluatedSadd("set:1", "member")))

        assertTrue(output.contains("val insertions_redis = redis()"))
        assertTrue(output.contains(".sadd(\"set:1\", \"member\")"))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testJavaFormatSingleSaddAction() {
        val output = writeJava(listOf(makeEvaluatedSadd("set:1", "member")))

        assertTrue(output.contains("List<RedisInsertionDto> insertions_redis = redis()"))
        assertTrue(output.contains(".sadd(\"set:1\", \"member\")"))
        assertTrue(output.contains("RedisInsertionResultsDto insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
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
        val output = writeKotlin(
            actions = listOf(makeEvaluatedSet("key:1", "val")),
            groupIndex = "_42"
        )

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

    @Test
    fun testMultipleSetActionsUsesAndChaining() {
        val actions = listOf(
            makeEvaluatedSet("key:1", "val1"),
            makeEvaluatedSet("key:2", "val2")
        )

        val output = writeKotlin(actions)

        assertTrue(output.contains(".and()"))
        assertTrue(output.contains("key:1"))
        assertTrue(output.contains("key:2"))
    }

    @Test
    fun testMixedActions() {
        val actions = listOf(
            makeEvaluatedSet("string:key", "val"),
            makeEvaluatedHset("hash:key", "field1", "val2"),
            makeEvaluatedSadd("set:key", "member")
        )

        val output = writeKotlin(actions)

        assertTrue(output.contains(".set(\"string:key\", \"val\")"))
        assertTrue(output.contains(".and()"))
        assertTrue(output.contains(".hset(\"hash:key\", \"field1\", \"val2\")"))
        assertTrue(output.contains(".sadd(\"set:key\", \"member\")"))
    }
}