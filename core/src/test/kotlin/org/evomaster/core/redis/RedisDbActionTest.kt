package org.evomaster.core.redis

import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    // --- RedisSaddAction ---

    @Test
    fun testSaddActionGetName() {
        val action = RedisSaddAction(key = "myset", memberGene = StringGene("member"))
        assertEquals("Redis_SADD_myset", action.getName())
    }

    @Test
    fun testSaddActionGetTargetKey() {
        val action = RedisSaddAction(key = "myset", memberGene = StringGene("member"))
        assertEquals("myset", action.getTargetKey())
    }

    @Test
    fun testSaddActionSeeTopGenesReturnsMemberGene() {
        val action = RedisSaddAction(key = "myset", memberGene = StringGene("member", "item1"))
        assertEquals(1, action.seeTopGenes().size)
        assertInstanceOf(StringGene::class.java, action.seeTopGenes()[0])
    }

    @Test
    fun testSaddActionCopyIsIndependent() {
        val original = RedisSaddAction(key = "myset", memberGene = StringGene("member", "item1"))
        val copy = original.copy() as RedisSaddAction
        copy.memberGene.value = "item2"

        assertEquals("item1", original.memberGene.value)
        assertEquals("item2", copy.memberGene.value)
    }

    // --- RedisSaddFromSinterAction ---

    @Test
    fun testSaddFromSinterActionGetName() {
        val action = RedisSaddFromSinterAction(keys = listOf("set1", "set2"), memberGene = StringGene("member"))
        assertEquals("Redis_SADD_SINTER_set1_set2", action.getName())
    }

    @Test
    fun testSaddFromSinterActionGetTargetKey() {
        val action = RedisSaddFromSinterAction(keys = listOf("set1", "set2"), memberGene = StringGene("member"))
        assertEquals("set1,set2", action.getTargetKey())
    }

    @Test
    fun testSaddFromSinterActionSeeTopGenesReturnsMemberGene() {
        val action = RedisSaddFromSinterAction(keys = listOf("set1", "set2"), memberGene = StringGene("member", "shared"))
        assertEquals(1, action.seeTopGenes().size)
        assertInstanceOf(StringGene::class.java, action.seeTopGenes()[0])
    }

    @Test
    fun testSaddFromSinterActionCopyIsIndependent() {
        val original = RedisSaddFromSinterAction(keys = listOf("set1", "set2"), memberGene = StringGene("member", "shared"))
        val copy = original.copy() as RedisSaddFromSinterAction
        copy.memberGene.value = "changed"

        assertEquals("shared", original.memberGene.value)
        assertEquals("changed", copy.memberGene.value)
        assertEquals(listOf("set1", "set2"), copy.keys)
    }

    @Test
    fun testSaddFromSinterRequiresNonEmptyKeys() {
        assertThrows<IllegalArgumentException> {
            RedisSaddFromSinterAction(keys = emptyList(), memberGene = StringGene("member"))
        }
    }
}