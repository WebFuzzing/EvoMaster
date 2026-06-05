package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a SET action, generated from a failed GET command.
 * (failed meaning GET commands that return no data when executed).
 *
 * @param key the new key to be inserted
 * @param valueGene gene representing the string value to insert
 */
class RedisSetAction(
    val key: String,
    val valueGene: StringGene
) : RedisDbAction() {

    init {
        addChildren(listOf(valueGene))
    }

    override fun getTargetKey() = key

    override fun seeTopGenes(): List<Gene> = listOf(valueGene)

    override fun copyContent(): Action =
        RedisSetAction(key, valueGene.copy() as StringGene)

    override fun getName() = "Redis_SET_${key}"
}