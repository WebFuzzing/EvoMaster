package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * Transforms a failed Redis command in an insert action.
 *
 * Example: GET key -> RedisDbAction(key, StringGene, RedisDataType STRING)
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
                // Only GET commands with a StringGene as value is supported at the moment.
                // More complex types will be included in the future.
                val valueGene = StringGene("value").also {
                    it.randomize(randomness, false)
                }
                RedisDbAction(
                    key = cmd.key,
                    valueGene = valueGene,
                    dataType = RedisDbAction.RedisDataType.STRING
                )
            }
    }
}
