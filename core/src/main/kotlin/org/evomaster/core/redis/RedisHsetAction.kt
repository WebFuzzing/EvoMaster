package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents an HSET action, generated from a failed HGET or HGETALL command
 * (failed meaning HGET or HGETALL commands that return no data when executed).
 *
 * <p>For HGET, [field] is the exact field observed in the failed command.
 * For HGETALL, [field] is a placeholder since the expected fields are unknown.
 *
 * @param key the new key to be inserted.
 * @param field the hash field to insert.
 * @param valueGene gene representing the field value to insert.
 */
class RedisHsetAction(
    val key: String,
    val field: String,
    val valueGene: StringGene
) : RedisDbAction() {

    init {
        addChildren(listOf(valueGene))
    }

    override fun getTargetKey() = key

    override fun seeTopGenes(): List<Gene> = listOf(valueGene)

    override fun copyContent(): Action =
        RedisHsetAction(key, field, valueGene.copy() as StringGene)

    override fun getName() = "Redis_HSET_${key}_$field"
}