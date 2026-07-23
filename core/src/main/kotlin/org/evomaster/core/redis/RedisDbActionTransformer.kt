package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.operations.*

/**
 * Transforms insert actions coming from failed commands into new commands to be executed in Redis.
 */
object RedisDbActionTransformer {

    fun transform(actions: List<RedisDbAction>): RedisDatabaseCommandsDto {
        val dto = RedisDatabaseCommandsDto()
        dto.insertions = actions.flatMap { action ->
            when (action) {
                is RedisSetAction -> listOf(RedisInsertionDto().also {
                    it.command = "SET"
                    it.key = action.key
                    it.value = action.valueGene.value
                })
                is RedisSetFromPatternAction -> listOf(RedisInsertionDto().also {
                    it.command = "SET"
                    it.key = action.keyGene.getValueAsRawString()
                    it.value = action.valueGene.value
                })
                is RedisHsetAction -> listOf(RedisInsertionDto().also {
                    it.command = "HSET"
                    it.key = action.key
                    it.field = action.field
                    it.value = action.valueGene.value
                })
                is RedisSaddAction -> listOf(RedisInsertionDto().also {
                    it.command = "SADD"
                    it.key = action.key
                    it.value = action.memberGene.value
                })
                is RedisSaddFromSinterAction -> action.keys.map { key ->
                    RedisInsertionDto().also {
                        it.command = "SADD"
                        it.key = key
                        it.value = action.memberGene.value
                    }
                }
            }
        }
        return dto
    }
}