package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RedisInsertBuilderTest {

    private val randomness = Randomness().apply { updateSeed(42) }

    private fun failedCommand(key: String) = RedisFailedCommand().also {
        it.key = key
        it.command = "GET"
    }

    @Test
    fun testBuildsActionForEachFailedCommand() {
        val commands = listOf(failedCommand("user:1"), failedCommand("user:2"))

        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet(), randomness)

        assertEquals(2, actions.size)
        assertEquals("user:1", actions[0].key)
        assertEquals("user:2", actions[1].key)
    }

    @Test
    fun testSkipsExistingKeys() {
        val commands = listOf(failedCommand("user:1"), failedCommand("user:2"))
        val existing = setOf("user:1")

        val actions = RedisInsertBuilder.buildInsertActions(commands, existing, randomness)

        assertEquals(1, actions.size)
        assertEquals("user:2", actions[0].key)
    }

    @Test
    fun testSkipsAllIfAllExist() {
        val commands = listOf(failedCommand("user:1"), failedCommand("user:2"))
        val existing = setOf("user:1", "user:2")

        val actions = RedisInsertBuilder.buildInsertActions(commands, existing, randomness)

        assertTrue(actions.isEmpty())
    }

    @Test
    fun testEmptyCommandsReturnsEmptyList() {
        val actions = RedisInsertBuilder.buildInsertActions(emptyList(), emptySet(), randomness)

        assertTrue(actions.isEmpty())
    }

    @Test
    fun testDefaultKeyspaceIsZero() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(failedCommand("key:1")), emptySet(), randomness
        )

        assertEquals("0", actions[0].keyspace)
    }

    @Test
    fun testDataTypeIsString() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(failedCommand("key:1")), emptySet(), randomness
        )

        assertEquals(RedisDbAction.RedisDataType.STRING, actions[0].dataType)
    }

    @Test
    fun testValueGeneIsInitialized() {
        val actions = RedisInsertBuilder.buildInsertActions(
            listOf(failedCommand("key:1")), emptySet(), randomness
        )

        val gene = actions[0].valueGene
        assertInstanceOf(StringGene::class.java, gene)
        // after randomize(), the value should not be the default empty string
        assertNotNull(gene.value)
    }

    @Test
    fun testEachActionHasIndependentGene() {
        val commands = listOf(failedCommand("key:1"), failedCommand("key:2"))

        val actions = RedisInsertBuilder.buildInsertActions(commands, emptySet(), randomness)

        // mutating one gene must not affect the other
        actions[0].valueGene.value = "mutated"
        assertNotEquals("mutated", actions[1].valueGene.value)
    }
}