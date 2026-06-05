package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.operations.*

/**
 * Transforms insert actions coming from failed commands into new commands to be executed in Redis.
 */
object RedisDbActionTransformer {

    fun transform(actions: List<RedisDbAction>): RedisDatabaseCommandsDto {
        val dto = RedisDatabaseCommandsDto()
        dto.insertions = actions.map { action ->
            when (action) {
                is RedisHsetAction -> RedisInsertionDto().also {
                    it.command = "HSET"
                    it.key = action.key
                    it.field = action.field
                    it.value = action.valueGene.value
                }
                is RedisSetAction -> RedisInsertionDto().also {
                    it.command = "SET"
                    it.key = action.key
                    it.value = action.valueGene.value
                }
                is RedisSetFromPatternAction -> RedisInsertionDto().also {
                    it.command = "SET"
                    it.key = action.keyGene.getValueAsRawString()
                    it.value = action.valueGene.value
                }
            }
        }
        return dto
    }
}