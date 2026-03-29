package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisDbActionTest {

    @Test
    fun testSeeTopGenesReturnsValueGene() {
        val action = RedisDbAction(
            key = "user:1",
            valueGene = StringGene("value"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        val genes = action.seeTopGenes()
        assertEquals(1, genes.size)
        assert(StringGene::class.isInstance(genes.first()))
    }

    @Test
    fun testGetName() {
        val action = RedisDbAction(
            key = "user:1",
            valueGene = StringGene("value"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        assertEquals("Redis_STRING_user:1", action.getName())
    }

    @Test
    fun testCopyContentPreservesFields() {
        val original = RedisDbAction(
            key = "session:abc",
            valueGene = StringGene("value", "someData"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        val copy = original.copy() as RedisDbAction

        assertEquals(original.key, copy.key)
        assertEquals(original.dataType, copy.dataType)
        assertEquals(original.valueGene.value, copy.valueGene.value)
    }

    @Test
    fun testCopyContentIsIndependent() {
        val original = RedisDbAction(
            key = "product:1",
            valueGene = StringGene("value", "original"),
            dataType = RedisDbAction.RedisDataType.STRING
        )

        val copy = original.copy() as RedisDbAction
        copy.valueGene.value = "modified"

        // mutating the copy must not affect the original
        assertEquals("original", original.valueGene.value)
        assertEquals("modified", copy.valueGene.value)
    }

}