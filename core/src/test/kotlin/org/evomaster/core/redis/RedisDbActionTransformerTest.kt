package org.evomaster.core.redis

import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisDbActionTransformerTest {

    @Test
    fun testTransformSetAction() {
        val action = RedisSetAction(key = "user:1", valueGene = StringGene("value", "Alice"))
        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        with(dto.insertions[0]) {
            assertEquals("SET", command)
            assertEquals("user:1", key)
            assertEquals("Alice", value)
        }
    }

    @Test
    fun testTransformHsetAction() {
        val action =
            RedisHsetAction(key = "user:1", field = "name", valueGene = StringGene("value", "Alice"))
        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        with(dto.insertions[0]) {
            assertEquals("HSET", command)
            assertEquals("user:1", key)
            assertEquals("name", field)
            assertEquals("Alice", value)
        }
    }

    @Test
    fun testTransformSetFromPatternAction() {
        val keyGene = RegexHandler.createGeneForJVM("^user:.*$")
        keyGene.fixedValue = "user:123"
        keyGene.usingFixedValue = true
        val action = RedisSetFromPatternAction(keyGene = keyGene, valueGene = StringGene("value", "data"))
        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        with(dto.insertions[0]) {
            assertEquals("SET", command)
            assertEquals("user:123", key)
            assertEquals("data", value)
        }
    }

    @Test
    fun testTransformMixedActions() {
        val actions = listOf(
            RedisSetAction(key = "k1", valueGene = StringGene("value", "v1")),
            RedisHsetAction(key = "k2", field = "f", valueGene = StringGene("value", "v2"))
        )
        val dto = RedisDbActionTransformer.transform(actions)
        assertEquals(2, dto.insertions.size)
    }

    @Test
    fun testTransformEmptyList() {
        assertEquals(0, RedisDbActionTransformer.transform(emptyList()).insertions.size)
    }
}