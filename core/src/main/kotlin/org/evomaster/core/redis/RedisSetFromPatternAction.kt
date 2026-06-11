package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents a SET action, generated from a failed KEYS command.
 * (failed meaning KEYS commands that return no data when executed).
 *
 * @param keyGene the new key to be inserted
 * @param valueGene gene representing the string value to insert
 */
class RedisSetFromPatternAction(
    val keyGene: RegexGene,
    val valueGene: StringGene
) : RedisDbAction() {

    init {
        addChildren(listOf(keyGene, valueGene))
    }

    override fun getTargetKey() = keyGene.getValueAsRawString()

    override fun seeTopGenes(): List<Gene> = listOf(keyGene, valueGene)

    override fun copyContent(): Action =
        RedisSetFromPatternAction(
            keyGene.copy() as RegexGene,
            valueGene.copy() as StringGene
        )

    override fun getName() = "Redis_SET_PATTERN_${keyGene.sourceRegex}"
}