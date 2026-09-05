package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery

/** Failed DynamoDB reads observed during one action. */
class DynamoDbExecution(val failedQueries: List<DynamoDbFailedQuery>) {

    companion object {

        /** Creates an execution view from the controller response, handling a missing response. */
        fun fromDto(dto: DynamoDbExecutionsDto?): DynamoDbExecution =
            DynamoDbExecution(dto?.failedQueries ?: emptyList())
    }
}
