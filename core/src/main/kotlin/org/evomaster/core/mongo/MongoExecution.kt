package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.execution.MongoFailedQuery
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
class MongoExecution(val failedQueries: MutableList<MongoFailedQuery>?) {

    companion object {

        fun fromDto(dto: MongoExecutionDto?): MongoExecution {
            return MongoExecution(dto?.failedQueries)
        }
    }
}