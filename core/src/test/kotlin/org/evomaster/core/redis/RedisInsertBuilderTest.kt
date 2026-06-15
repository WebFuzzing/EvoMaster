package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
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

    // --- helpers ---

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
}