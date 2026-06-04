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
        return failedCommands.flatMap { cmd ->
            buildActionsForCommand(cmd, existingKeys, randomness)
        }
    }

    private fun buildActionsForCommand(
        cmd: RedisFailedCommand,
        existingKeys: Set<String>,
        randomness: Randomness
    ): List<RedisDbAction> {
        val keys = cmd.keys ?: emptyList()

        return when (cmd.command) {
            "GET" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(RedisSetAction(
                    keyGene = StringGene("key", key),
                    valueGene = StringGene("value").also { it.randomize(randomness, false) }
                ))
            }
            "HGET" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(RedisHsetAction(
                    keyGene = StringGene("key", key),
                    field = cmd.field ?: "field",
                    valueGene = StringGene("value").also { it.randomize(randomness, false) }
                ))
            }
            "HGETALL" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(RedisHsetAction(
                    keyGene = StringGene("key", key),
                    field = "field",
                    valueGene = StringGene("value").also { it.randomize(randomness, false) }
                ))
            }
            "KEYS" -> {
                val keyGene = StringGene("key").also {
                    it.addSpecializations(
                        "key",
                        listOf(StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, cmd.pattern)),
                        randomness,
                        updateGlobalInfo = false,
                        enableConstraintHandling = false
                    )
                    it.doInitialize(randomness)
                }
                listOf(RedisSetAction(
                    keyGene = keyGene,
                    valueGene = StringGene("value").also { it.randomize(randomness, false) }
                ))
            }
            "SMEMBERS" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(RedisSaddAction(
                    keyGene = StringGene("key", key),
                    memberGene = StringGene("member").also { it.randomize(randomness, false) }
                ))
            }
            "SINTER" -> {
                if (keys.isEmpty()) return emptyList()
                val sharedElement = StringGene("member").also { it.randomize(randomness, false) }
                keys.map { key ->
                    RedisSaddAction(
                        keyGene = StringGene("key", key),
                        memberGene = sharedElement.copy() as StringGene
                    )
                }
            }
            else -> emptyList()
        }
    }
}