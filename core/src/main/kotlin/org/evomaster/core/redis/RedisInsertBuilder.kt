package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * Transforms a failed Redis command in an insert action.
 *
 * Example: GET key -> RedisDbAction(key, SET, StringGene)
 */
object RedisInsertBuilder {

    fun buildInsertActions(
        failedCommands: List<RedisFailedCommand>,
        existingKeys: Set<String>,
        randomness: Randomness
    ): List<RedisDbAction> {

        return failedCommands
            .filter { it.key !in existingKeys }
            .map { cmd ->
                val valueGene = StringGene("value").also {
                    it.randomize(randomness, false)
                }
                RedisDbAction(
                    keyspace = "0",
                    key = cmd.key,
                    valueGene = valueGene,
                    dataType = RedisDbAction.RedisDataType.STRING
                )
            }
    }
}
