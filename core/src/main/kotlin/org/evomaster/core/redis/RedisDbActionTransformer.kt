package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.operations.*

/**
 * Transforms insert actions coming from failed commands into new commands to be executed in Redis.
 */
object RedisDbActionTransformer {

    fun transform(actions: List<RedisDbAction>): RedisDatabaseCommandsDto {
        val dto =
            RedisDatabaseCommandsDto()
        dto.insertions = actions.map { action ->
            // Current version supports only GET to SET commands. Command keyword is not included at the moment.
            RedisInsertionDto().also {
                it.key = action.key
                it.value = action.valueGene.value
            }
        }
        return dto
    }
}