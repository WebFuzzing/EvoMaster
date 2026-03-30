package org.evomaster.core.redis

import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Represents an action to insert data into a Redis database, generated in response
 * to a failed Redis read command.
 */
class RedisDbAction(

    /**
     * Immutable key for which Redis returned no data.
     */
    val key: String,
    /**
     * Value associated to the key.
     */
    val valueGene: StringGene,
    /**
     * Command type executed.
     */
    val dataType: RedisDataType
) : EnvironmentAction(listOf()) {

    enum class RedisDataType {
        STRING
    }

    override fun seeTopGenes(): List<Gene> = listOf(valueGene)

    override fun copyContent(): Action {
        return RedisDbAction(
            key,
            valueGene.copy() as StringGene,
            dataType
        )
    }

    override fun getName(): String = "Redis_${dataType}_${key}"
}