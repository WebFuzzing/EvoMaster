package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Transforms a failed Redis read command into the corresponding insert action.
 *
 * Each supported command type maps to a specific [RedisDbAction] subclass:
 * - GET key        -> [RedisSetAction]
 * - KEYS pattern   -> [RedisSetFromPatternAction]
 * - HGET key field -> [RedisHsetAction]
 * - HGETALL key    -> [RedisHsetAction]
 */
object RedisInsertBuilder {

    private val log: Logger = LoggerFactory.getLogger(RedisInsertBuilder::class.java)

    fun buildInsertActions(
        failedCommands: List<RedisFailedCommand>,
        existingKeys: Set<String>
    ): List<RedisDbAction> {
        return failedCommands
            .filter { it.key == null || it.key !in existingKeys }
            .flatMap { cmd -> buildActionsForCommand(cmd) }
    }

    private fun buildActionsForCommand(
        cmd: RedisFailedCommand
    ): List<RedisDbAction> {
        return when (cmd.command) {
            "GET" -> listOf(RedisSetAction(
                key = cmd.key!!,
                valueGene = StringGene("value")
            ))
            "HGET" -> listOf(RedisHsetAction(
                key = cmd.key!!,
                field = cmd.field ?: "field",
                valueGene = StringGene("value")
            ))
            "HGETALL" -> listOf(RedisHsetAction(
                key = cmd.key!!,
                field = "field",
                valueGene = StringGene("value")
            ))
            "KEYS" -> {
                val pattern = cmd.pattern ?: return emptyList()
                val keyGene = RegexHandler.createGeneForJVM(pattern)
                listOf(RedisSetFromPatternAction(
                    keyGene = keyGene,
                    valueGene = StringGene("value")
                ))
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "Unsupported Redis command for insert action: ${cmd.command}")
                assert(false) { "Unsupported Redis command: ${cmd.command}" }
                emptyList()
            }
        }
    }
}