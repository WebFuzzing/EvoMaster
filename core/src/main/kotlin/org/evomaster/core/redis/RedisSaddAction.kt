package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a SADD action, generated from a failed SMEMBERS command
 * (failed meaning those commands return no data when executed).
 *
 * <p>For SMEMBERS, a single [RedisSaddAction] is generated for the observed key,
 * inserting one element so the set is non-empty.
 *
 * @param key the key of the set to insert into.
 * @param memberGene gene representing the element to add to the set.
 */
class RedisSaddAction(
    val key: String,
    val memberGene: StringGene
) : RedisDbAction() {

    init {
        addChildren(listOf(memberGene))
    }

    override fun getTargetKey() = key

    override fun seeTopGenes(): List<Gene> = listOf(memberGene)

    override fun copyContent(): Action =
        RedisSaddAction(key, memberGene.copy() as StringGene)

    override fun getName() = "Redis_SADD_${key}"
}