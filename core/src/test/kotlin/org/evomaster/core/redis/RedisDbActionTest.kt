package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class RedisDbActionTest {

    // --- RedisSetAction ---

    @Test
    fun testSetActionGetName() {
        val action = RedisSetAction(key = "user:1", valueGene = StringGene("value"))
        assertEquals("Redis_SET_user:1", action.getName())
    }

    @Test
    fun testSetActionGetTargetKey() {
        val action = RedisSetAction(key = "user:1", valueGene = StringGene("value"))
        assertEquals("user:1", action.getTargetKey())
    }

    @Test
    fun testSetActionSeeTopGenesReturnsValueGeneOnly() {
        val action = RedisSetAction(key = "user:1", valueGene = StringGene("value", "Alice"))
        assertEquals(1, action.seeTopGenes().size)
        assertInstanceOf(StringGene::class.java, action.seeTopGenes()[0])
    }

    @Test
    fun testSetActionCopyIsIndependent() {
        val original = RedisSetAction(key = "k", valueGene = StringGene("value", "original"))
        val copy = original.copy() as RedisSetAction
        copy.valueGene.value = "modified"

        assertEquals("original", original.valueGene.value)
        assertEquals("modified", copy.valueGene.value)
    }

    // --- RedisHsetAction ---

    @Test
    fun testHsetActionGetName() {
        val action = RedisHsetAction(key = "user:1", field = "name", valueGene = StringGene("value"))
        assertEquals("Redis_HSET_user:1_name", action.getName())
    }

    @Test
    fun testHsetActionGetTargetKey() {
        val action = RedisHsetAction(key = "user:1", field = "name", valueGene = StringGene("value"))
        assertEquals("user:1", action.getTargetKey())
    }

    @Test
    fun testHsetActionFieldIsFixed() {
        val action = RedisHsetAction(key = "user:1", field = "email", valueGene = StringGene("value"))
        assertEquals("email", action.field)
    }

    @Test
    fun testHsetActionCopyIsIndependent() {
        val original = RedisHsetAction(key = "user:1", field = "name", valueGene = StringGene("value", "original"))
        val copy = original.copy() as RedisHsetAction
        copy.valueGene.value = "modified"

        assertEquals("original", original.valueGene.value)
        assertEquals("modified", copy.valueGene.value)
    }

}