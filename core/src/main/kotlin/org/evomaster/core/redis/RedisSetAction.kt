package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a SET action, generated from a failed GET command.
 * (failed meaning GET commands that return no data when executed).
 *
 * @param keyGene the new key to be inserted
 * @param valueGene gene representing the string value to insert
 */
class RedisSetAction(
    val keyGene: StringGene,
    val valueGene: StringGene,
    private val nameKey: String = keyGene.value
) : RedisDbAction() {

    init {
        addChildren(listOf(keyGene, valueGene))
    }

    override fun getTargetKey() = keyGene.value

    override fun seeTopGenes(): List<Gene> = listOf(keyGene, valueGene)

    override fun copyContent(): Action =
        RedisSetAction(keyGene.copy() as StringGene, valueGene.copy() as StringGene, nameKey)

    override fun getName() = "Redis_SET_${nameKey}"
}