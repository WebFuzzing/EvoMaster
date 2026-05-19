package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RedisInsertBuilderTest {

    private val randomness = Randomness().apply { updateSeed(42) }

    private fun getCommand(key: String) = RedisFailedCommand().also {
        it.key = key
        it.command = "GET"
    }

    private fun hgetCommand(key: String, field: String) = RedisFailedCommand().also {
        it.key = key
        it.field = field
        it.command = "HGET"
    }

    private fun hgetallCommand(key: String) = RedisFailedCommand().also {
        it.key = key
        it.command = "HGETALL"
    }

    private fun keysCommand(pattern: String) = RedisFailedCommand().also {
        it.pattern = pattern
        it.command = "KEYS"
    }

    // --- GET ---

    @Test
    fun testGetBuildsSetActionForEachCommand() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))

        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet(), randomness)

        assertEquals(2, actions.size)
        actions.forEach { assertTrue(it is RedisSetAction) }
        assertEquals("user:1", (actions[0] as RedisSetAction).keyGene.value)
        assertEquals("user:2", (actions[1] as RedisSetAction).keyGene.value)
    }

    @Test
    fun testGetSkipsExistingKeys() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))

        val actions = RedisInsertBuilder.buildInsertActions(commands, setOf("user:1"), randomness)

        assertEquals(1, actions.size)
        assertEquals("user:2", (actions[0] as RedisSetAction).keyGene.value)
    }

    @Test
    fun testGetKeyGeneInitializedWithObservedKey() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(getCommand("known:key")), emptySet(), randomness
        )

        val action = actions[0] as RedisSetAction
        assertEquals("known:key", action.keyGene.value)
    }

    // --- HGET ---

    @Test
    fun testHgetBuildsHsetAction() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetCommand("user:1", "name")), emptySet(), randomness
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.keyGene.value)
        assertEquals("name", action.field)
    }

    // --- HGETALL ---

    @Test
    fun testHgetallBuildsHsetActionWithPlaceholderField() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(hgetallCommand("user:1")), emptySet(), randomness
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisHsetAction
        assertEquals("user:1", action.keyGene.value)
        assertEquals("field", action.field)
    }

    // --- KEYS ---

    @Test
    fun testKeysBuildsSetActionWithRegexSpecialization() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(keysCommand("^user:.*$")), emptySet(), randomness
        )

        assertEquals(1, actions.size)
        val action = actions[0] as RedisSetAction
        // key gene should have a specialization from the pattern
        assertFalse(action.keyGene.specializationGenes.isEmpty())
    }

    // --- general ---

    @Test
    fun testSkipsAllIfAllExist() {
        val commands = listOf(getCommand("user:1"), getCommand("user:2"))

        val actions = RedisInsertBuilder.buildInsertActions(
            commands, setOf("user:1", "user:2"), randomness
        )

        assertTrue(actions.isEmpty())
    }

    @Test
    fun testEmptyCommandsReturnsEmptyList() {
        val actions = RedisInsertBuilder.buildInsertActions(emptyList(), emptySet(), randomness)

        assertTrue(actions.isEmpty())
    }

    @Test
    fun testValueGeneIsInitialized() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(getCommand("key:1")), emptySet(), randomness
        )

        val gene = (actions[0] as RedisSetAction).valueGene
        assertInstanceOf(StringGene::class.java, gene)
        assertNotNull(gene.value)
    }

    @Test
    fun testEachActionHasIndependentValueGene() {
        val commands = listOf(getCommand("key:1"), getCommand("key:2"))

        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet(), randomness)

        (actions[0] as RedisSetAction).valueGene.value = "mutated"
        assertNotEquals("mutated", (actions[1] as RedisSetAction).valueGene.value)
    }

    @Test
    fun testUnsupportedCommandSkippedSilently() {
        val unsupported = RedisFailedCommand().also {
            it.key = "key:1"
            it.command = "UNKNOWN_CMD"
        }

        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(unsupported), emptySet(), randomness
        )

        assertTrue(actions.isEmpty())
    }
}