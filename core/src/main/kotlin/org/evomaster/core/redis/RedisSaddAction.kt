package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a SADD action, generated from a failed SMEMBERS or SINTER command
 * (failed meaning those commands return no data when executed).
 *
 * <p>For SMEMBERS, a single [RedisSaddAction] is generated for the observed key,
 * inserting one element so the set is non-empty.
 *
 * <p>For SINTER, one [RedisSaddAction] is generated per key involved in the
 * intersection, all sharing the same [elementGene] value so the intersection
 * is guaranteed to be non-empty.
 *
 * @param keyGene the key of the set to insert into.
 * @param elementGene gene representing the element to add to the set.
 */
class RedisSaddAction(
    val keyGene: StringGene,
    val memberGene: StringGene,
    private val nameKey: String = keyGene.value
) : RedisDbAction() {

    init {
        addChildren(listOf(keyGene, memberGene))
    }

    override fun getTargetKey() = keyGene.value

    override fun seeTopGenes(): List<Gene> = listOf(keyGene, memberGene)

    override fun copyContent(): Action =
        RedisSaddAction(keyGene.copy() as StringGene, memberGene.copy() as StringGene, nameKey)

    override fun getName() = "Redis_SADD_${nameKey}"
}