package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisDbActionTransformerTest {

    @Test
    fun testTransformSingleAction() {
        val action = RedisDbAction(
            keyspace = "0",
            key = "user:1",
            valueGene = StringGene("value", "Alice"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        assertEquals("user:1", dto.insertions[0].key)
        assertEquals("Alice", dto.insertions[0].value)
        assertEquals(0, dto.insertions[0].keyspace)
    }

    @Test
    fun testTransformMultipleActions() {
        val actions = listOf(
            RedisDbAction("0", "product:1", StringGene("value", "chair"), RedisDbAction.RedisDataType.STRING),
            RedisDbAction("0", "product:2", StringGene("value", "table"), RedisDbAction.RedisDataType.STRING)
        )

        val dto = RedisDbActionTransformer.transform(actions)

        assertEquals(2, dto.insertions.size)
        assertEquals("product:1", dto.insertions[0].key)
        assertEquals("chair", dto.insertions[0].value)
        assertEquals("product:2", dto.insertions[1].key)
        assertEquals("table", dto.insertions[1].value)
    }

    @Test
    fun testTransformPreservesKeyspace() {
        val action = RedisDbAction(
            keyspace = "3",
            key = "session:abc",
            valueGene = StringGene("value", "data"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(3, dto.insertions[0].keyspace)
    }

    @Test
    fun testTransformEmptyList() {
        val dto = RedisDbActionTransformer.transform(emptyList())

        assertEquals(0, dto.insertions.size)
    }

    @Test
    fun testTransformPreservesOrder() {
        val keys = listOf("a", "b", "c", "d")
        val actions = keys.map {
            RedisDbAction("0", it, StringGene("value", "v_$it"), RedisDbAction.RedisDataType.STRING)
        }

        val dto = RedisDbActionTransformer.transform(actions)

        keys.forEachIndexed { index, key ->
            assertEquals(key, dto.insertions[index].key)
        }
    }
}