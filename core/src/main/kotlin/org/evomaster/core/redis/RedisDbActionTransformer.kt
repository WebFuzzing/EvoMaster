package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.operations.*

object RedisDbActionTransformer {

    fun transform(actions: List<RedisDbAction>): RedisDatabaseCommandDto {
        val dto = RedisDatabaseCommandDto()
        dto.insertions = actions.map { action ->
            RedisInsertionDto().also {
                it.key = action.key
                it.value = action.valueGene.value
                it.keyspace = action.keyspace.toInt()
            }
        }
        return dto
    }
}