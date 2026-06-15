package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a single SADD action generated from a failed SINTER command.
 * Instead of one action per key, this action holds all keys involved in the
 * intersection and a single [memberGene] that will be added to every set,
 * guaranteeing a non-empty intersection.
 *
 * @param keys all keys involved in the SINTER command.
 * @param memberGene gene representing the element to insert into each set.
 */
class RedisSaddFromSinterAction(
    val keys: List<String>,
    val memberGene: StringGene
) : RedisDbAction() {

    init {
        require(keys.isNotEmpty()) { "RedisSaddFromSinterAction requires at least one key" }
        addChildren(listOf(memberGene))
    }

    override fun getTargetKey() = keys.joinToString(",")

    override fun insertionsCount(): Int = keys.size

    override fun seeTopGenes(): List<Gene> = listOf(memberGene)

    override fun copyContent(): Action =
        RedisSaddFromSinterAction(keys, memberGene.copy() as StringGene)

    override fun getName() = "Redis_SADD_SINTER_${keys.joinToString("_")}"
}