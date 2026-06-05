package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RedisInsertBuilderTest {

    // --- GET ---

    @Test
    fun testGetBuildsSetAction() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(getCommand("user:1")), emptySet()
        )
        assertEquals(1, actions.size)
        assertEquals("user:1", (actions[0] as RedisSetAction).key)
    }

    @Test
    fun testGetSkipsExistingKey() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(getCommand("user:1"), getCommand("user:2")), setOf("user:1")
        )
        assertEquals(1, actions.size)
        assertEquals("user:2", (actions[0] as RedisSetAction).key)
    }

    // --- HGET ---

    @Test
    fun testHgetBuildsHsetActionWithObservedField() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetCommand("user:1", "name")), emptySet()
        )
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.key)
        assertEquals("name", action.field)
    }

    // --- HGETALL ---

    @Test
    fun testHgetallBuildsHsetActionWithPlaceholderField() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("user:1")), emptySet()
        )
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.key)
        assertEquals("field", action.field)
    }

    // --- KEYS ---

    @Test
    fun testKeysBuildsSetFromPatternAction() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(keysCommand("^user:.*$")), emptySet()
        )
        assertEquals(1, actions.size)
        assertInstanceOf(RedisSetFromPatternAction::class.java, actions[0])
        assertEquals("^user:.*$", (actions[0] as RedisSetFromPatternAction).keyGene.sourceRegex)
    }

    @Test
    fun testSkipsAllIfAllExist() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(getCommand("user:1"), getCommand("user:2")), setOf("user:1", "user:2")
        )
        assertTrue(actions.isEmpty())
    }

    @Test
    fun testEmptyCommandsReturnsEmptyList() {
        assertTrue(RedisInsertBuilder.buildInsertActions(emptyList(), emptySet()).isEmpty())
    }

    @Test
    fun testUnsupportedCommandThrowsAssertionError() {
        val cmd = RedisFailedCommand().also { it.key = "k"; it.command = "UNKNOWN" }

        assertThrows<AssertionError> {
            RedisInsertBuilder.buildInsertActions(listOf(cmd), emptySet())
        }
    }

    private fun getCommand(key: String) =
        RedisFailedCommand().also { it.key = key; it.command = "GET" }
    private fun hgetCommand(key: String, field: String) =
        RedisFailedCommand().also { it.key = key; it.field = field; it.command = "HGET" }
    private fun hgetallCommand(key: String) =
        RedisFailedCommand().also { it.key = key; it.command = "HGETALL" }
    private fun keysCommand(pattern: String) =
        RedisFailedCommand().also { it.pattern = pattern; it.command = "KEYS" }
}