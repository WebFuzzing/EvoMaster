package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents an HSET action, generated from a failed HGET or HGETALL command.
 *
 * <p>For HGET, [field] is the exact field observed in the failed command.
 * For HGETALL, [field] is a placeholder since the expected fields are unknown.
 *
 * @param keyGene the new key to be inserted.
 * @param field the hash field to insert.
 * @param valueGene gene representing the field value to insert.
 */
class RedisHsetAction(
    val keyGene: StringGene,
    val field: String,
    val valueGene: StringGene
) : RedisDbAction() {

    override fun getTargetKey() = keyGene.value

    override fun seeTopGenes(): List<Gene> = listOf(keyGene, valueGene)

    override fun copyContent(): Action =
        RedisHsetAction(keyGene, field, valueGene.copy() as StringGene)

    override fun getName() = "Redis_HSET_${keyGene.value}_$field"
}