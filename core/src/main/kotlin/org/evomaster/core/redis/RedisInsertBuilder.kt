package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * Transforms a failed Redis read command into the corresponding insert action.
 *
 * Each supported command type maps to a specific [RedisDbAction] subclass:
 * - GET key        -> [RedisSetAction]
 * - KEYS pattern   -> [RedisSetAction]
 * - HGET key field -> [RedisHsetAction]
 * - HGETALL key    -> [RedisHsetAction]
 */
object RedisInsertBuilder {

    fun buildInsertActions(
        failedCommands: List<RedisFailedCommand>,
        existingKeys: Set<String>,
        randomness: Randomness
    ): List<RedisDbAction> {

        return failedCommands
            .filter { it.key == null || it.key !in existingKeys }
            .mapNotNull { cmd ->
                when (cmd.command) {
                    "GET" -> {
                        val keyGene = StringGene("key", cmd.key)
                        val valueGene = StringGene("value").also {
                            it.randomize(randomness, false)
                        }
                        RedisSetAction(
                            keyGene = keyGene,
                            valueGene = valueGene
                        )
                    }
                    "HGET" -> {
                        val keyGene = StringGene("key", cmd.key)
                        val valueGene = StringGene("value").also {
                            it.randomize(randomness, false)
                        }
                        RedisHsetAction(
                            keyGene = keyGene,
                            field = cmd.field,
                            valueGene = valueGene
                        )
                    }
                    "HGETALL" -> {
                        val keyGene = StringGene("key", cmd.key)
                        val valueGene = StringGene("value").also {
                            it.randomize(randomness, false)
                        }
                        // field is unknown — using a placeholder so the key exists in Redis
                        RedisHsetAction(
                            keyGene = keyGene,
                            field = "field",
                            valueGene = valueGene
                        )
                    }
                    "KEYS" -> {
                        val keyGene = StringGene("key").also {
                            it.addSpecializations("key",
                                listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, cmd.pattern)),
                                randomness,
                                updateGlobalInfo = false,  // should this be false?
                                enableConstraintHandling = false)
                            it.doInitialize(randomness)
                        }
                        val valueGene = StringGene("value").also {
                            it.randomize(randomness, false)
                        }
                        RedisSetAction(
                            keyGene = keyGene,
                            valueGene = valueGene
                        )
                    }
                    else -> {
                        // unsupported command
                        null
                    }
                }
            }
    }
}