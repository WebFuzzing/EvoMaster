package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisDbActionTest {

    // --- RedisSetAction ---

    @Test
    fun testSetActionSeeTopGenesReturnsBothGenes() {
        val action = RedisSetAction(
            keyGene = StringGene("key", "user:1"),
            valueGene = StringGene("value", "Alice")
        )

        val genes = action.seeTopGenes()
        assertEquals(2, genes.size)
        assert(StringGene::class.isInstance(genes[0]))
        assert(StringGene::class.isInstance(genes[1]))
    }

    @Test
    fun testSetActionGetName() {
        val action = RedisSetAction(
            keyGene = StringGene("key", "user:1"),
            valueGene = StringGene("value")
        )

        assertEquals("Redis_SET_user:1", action.getName())
    }

    @Test
    fun testSetActionCopyPreservesFields() {
        val original = RedisSetAction(
            keyGene = StringGene("key", "session:abc"),
            valueGene = StringGene("value", "someData")
        )

        val copy = original.copy() as RedisSetAction

        assertEquals(original.keyGene.value, copy.keyGene.value)
        assertEquals(original.valueGene.value, copy.valueGene.value)
    }

    @Test
    fun testSetActionCopyIsIndependent() {
        val original = RedisSetAction(
            keyGene = StringGene("key", "product:1"),
            valueGene = StringGene("value", "original")
        )

        val copy = original.copy() as RedisSetAction
        copy.valueGene.value = "modified"

        assertEquals("original", original.valueGene.value)
        assertEquals("modified", copy.valueGene.value)
    }

    @Test
    fun testSetActionGetTargetKey() {
        val action = RedisSetAction(
            keyGene = StringGene("key", "user:1"),
            valueGene = StringGene("value")
        )

        assertEquals("user:1", action.getTargetKey())
    }

    // --- RedisHsetAction ---

    @Test
    fun testHsetActionSeeTopGenesReturnsBothGenes() {
        val action = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "name",
            valueGene = StringGene("value", "Alice")
        )

        val genes = action.seeTopGenes()
        assertEquals(2, genes.size)
    }

    @Test
    fun testHsetActionGetName() {
        val action = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "name",
            valueGene = StringGene("value")
        )

        assertEquals("Redis_HSET_user:1_name", action.getName())
    }

    @Test
    fun testHsetActionCopyPreservesFields() {
        val original = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "age",
            valueGene = StringGene("value", "30")
        )

        val copy = original.copy() as RedisHsetAction

        assertEquals(original.keyGene.value, copy.keyGene.value)
        assertEquals(original.field, copy.field)
        assertEquals(original.valueGene.value, copy.valueGene.value)
    }

    @Test
    fun testHsetActionCopyIsIndependent() {
        val original = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "name",
            valueGene = StringGene("value", "original")
        )

        val copy = original.copy() as RedisHsetAction
        copy.valueGene.value = "modified"

        assertEquals("original", original.valueGene.value)
        assertEquals("modified", copy.valueGene.value)
    }

    @Test
    fun testHsetActionFieldIsImmutable() {
        val action = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "email",
            valueGene = StringGene("value")
        )

        assertEquals("email", action.field)
    }

    @Test
    fun testHsetActionGetTargetKey() {
        val action = RedisHsetAction(
            keyGene = StringGene("key", "user:1"),
            field = "name",
            valueGene = StringGene("value")
        )

        assertEquals("user:1", action.getTargetKey())
    }
}