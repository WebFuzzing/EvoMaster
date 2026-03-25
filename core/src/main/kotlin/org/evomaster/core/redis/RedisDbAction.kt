package org.evomaster.core.redis

import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene

class RedisDbAction(

    /**
     * The database logical index.
     */
    val keyspace: String,
    /**
     * Immutable key corresponding to the failed command.
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
            keyspace,
            key,
            valueGene.copy() as StringGene,
            dataType
        )
    }

    override fun getName(): String = "Redis_${dataType}_${key}"
}