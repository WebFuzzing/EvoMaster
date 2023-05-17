package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.execution.FailedQuery
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
class MongoExecution(val failedQueries: List<FailedQuery>) {

    companion object {

        fun fromDto(dto: MongoExecutionDto?): MongoExecution {
            return MongoExecution(dto!!.failedQueries)
        }
    }
}