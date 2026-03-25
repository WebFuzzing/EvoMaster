package org.evomaster.core.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand

class RedisExecution(val failedCommands: MutableList<RedisFailedCommand>?) {

    companion object {

        fun fromDto(dto: RedisExecutionsDto?): RedisExecution {
            return RedisExecution(dto?.failedCommands)
        }
    }
}
