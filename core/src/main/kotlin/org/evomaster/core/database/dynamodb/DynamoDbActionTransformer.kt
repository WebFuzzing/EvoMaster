package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDatabaseCommandsDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto

/**
 * Transforms DynamoDB actions into controller insertion commands.
 */
object DynamoDbActionTransformer {

    /**
     * Transforms evaluated actions into insertion DTOs grouped in a database command payload.
     *
     * @param actions list of DynamoDB actions to transform
     * @return database commands DTO containing all converted insertions
     */
    fun transform(actions: List<DynamoDbAction>): DynamoDbDatabaseCommandsDto =
        DynamoDbDatabaseCommandsDto().also { commands ->
            commands.insertions = actions.map { action ->
                DynamoDbInsertionDto().also { insertion ->
                    insertion.tableName = action.tableName
                    insertion.attributes = action.attributes.map { it.toDto() }
                }
            }
        }
}
