package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisDbActionTransformerTest {

    @Test
    fun testTransformSingleHsetAction() {
        val action = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "name",
            valueGene = StringGene("value", "Alice")
        )

        val dto = RedisDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        assertEquals("HSET", dto.insertions[0].command)
        assertEquals("user:1", dto.insertions[0].key)
        assertEquals("name", dto.insertions[0].field)
        assertEquals("Alice", dto.insertions[0].value)
    }

    @Test
    fun testTransformEmptyList() {
        val dto = RedisDbActionTransformer.transform(emptyList())

        assertEquals(0, dto.insertions.size)
    }
}