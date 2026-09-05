package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDatabaseCommandsDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene

/** Transforms DynamoDB actions into controller insertion commands. */
object DynamoDbActionTransformer {

    /** Converts initialization actions to the controller's DynamoDB insertion DTO. */
    fun transform(actions: List<DynamoDbAction>): DynamoDbDatabaseCommandsDto =
        DynamoDbDatabaseCommandsDto().also { commands ->
            commands.insertions = actions.map { action ->
                DynamoDbInsertionDto().also { insertion ->
                    insertion.tableName = action.tableName
                    insertion.attributes = action.attributes.map { attribute ->
                        DynamoDbAttributeValueDto(
                            attribute.attributeName,
                            attribute.type,
                            when (attribute.type) {
                                DynamoDbScalarTypeDto.STRING -> (attribute.gene as StringGene).value
                                DynamoDbScalarTypeDto.NUMBER -> (attribute.gene as BigDecimalGene).value.toPlainString()
                                DynamoDbScalarTypeDto.BOOLEAN -> (attribute.gene as BooleanGene).value.toString()
                            }
                        )
                    }
                }
            }
        }
}
