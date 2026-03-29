package org.evomaster.core.output

import org.evomaster.core.redis.RedisDbAction
import org.evomaster.core.redis.RedisDbActionResult
import org.evomaster.core.search.action.EvaluatedRedisDbAction

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RedisWriterTest {

    private fun makeEvaluated(
        key: String,
        value: String,
        success: Boolean = true
    ): EvaluatedRedisDbAction {
        val action = RedisDbAction(
            key = key,
            valueGene = StringGene("value", value),
            dataType = RedisDbAction.RedisDataType.STRING
        )
        action.setLocalId("test-redis-action")
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
    fun testSkipFailureFiltersFailedInsertions() {
        val actions = listOf(
            makeEvaluated("key:1", "val1", success = true),
            makeEvaluated("key:2", "val2", success = false)
        )

        val output = writeKotlin(actions, skipFailure = true)

        assertTrue(output.contains("key:1"))
        assertFalse(output.contains("key:2"))
    }

    @Test
    fun testSkipFailureAllFailedGeneratesNothing() {
        val actions = listOf(makeEvaluated("key:1", "val1", success = false))

        val output = writeKotlin(actions, skipFailure = true)

        assertTrue(output.isBlank())
    }

    @Test
    fun testKotlinFormatSingleAction() {
        val output = writeKotlin(listOf(makeEvaluated("user:1", "Alice")))

        assertTrue(output.contains("val insertions_redis = redis()"))
        assertTrue(output.contains(".set(\"user:1\", \"Alice\", 0)"))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testJavaFormatSingleAction() {
        val output = writeJava(listOf(makeEvaluated("user:1", "Alice")))

        assertTrue(output.contains("List<RedisInsertionDto> insertions_redis = redis()"))
        assertTrue(output.contains(".set(\"user:1\", \"Alice\", 0)"))
        assertTrue(output.contains("RedisInsertionResultsDto insertions_redis_result = controller.execInsertionsIntoRedisDatabase(insertions_redis)"))
    }

    @Test
    fun testKotlinEscapesDollarSign() {
        val output = writeKotlin(listOf(makeEvaluated("key", "\$HOME")))

        assertTrue(output.contains("\\${'$'}HOME") || output.contains("\\\$HOME"))
        assertFalse(output.contains("\"\$HOME\""))
    }

    @Test
    fun testKeyWithSpecialCharactersIsEscaped() {
        val output = writeKotlin(listOf(makeEvaluated("key\"with\"quotes", "value")))

        assertTrue(output.contains("key\\\"with\\\"quotes"))
    }

    @Test
    fun testGroupIndexAppearsInVariableName() {
        val output = writeKotlin(
            actions = listOf(makeEvaluated("key:1", "val")),
            groupIndex = "_42"
        )

        assertTrue(output.contains("insertions_redis_42"))
    }

    @Test
    fun testInsertionVarsAccumulated() {
        val insertionVars = mutableListOf<Pair<String, String>>()
        writeKotlin(listOf(makeEvaluated("key:1", "val")), insertionVars = insertionVars)

        assertEquals(1, insertionVars.size)
        assertEquals("insertions_redis", insertionVars[0].first)
        assertEquals("insertions_redis_result", insertionVars[0].second)
    }

    @Test
    fun testMultipleActionsUsesAndChaining() {
        val actions = listOf(
            makeEvaluated("key:1", "val1"),
            makeEvaluated("key:2", "val2")
        )

        val output = writeKotlin(actions)

        assertTrue(output.contains(".and()"))
        assertTrue(output.contains("key:1"))
        assertTrue(output.contains("key:2"))
    }
}