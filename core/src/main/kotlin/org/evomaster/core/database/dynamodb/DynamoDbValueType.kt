package org.evomaster.core.database.dynamodb

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto

/**
 * Core representation of native DynamoDB attribute value types.
 */
enum class DynamoDbValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    MAP,
    LIST,
    STRING_SET,
    NUMBER_SET,
    NULL;

    /**
     * Converts this core type to the controller DTO enum.
     */
    fun toDto(): DynamoDbValueTypeDto = DynamoDbValueTypeDto.valueOf(name)

    companion object {
        /**
         * Converts a controller [DynamoDbValueTypeDto] to a core [DynamoDbValueType].
         */
        fun fromDto(dto: DynamoDbValueTypeDto): DynamoDbValueType = valueOf(dto.name)

        /**
         * Converts a controller [DynamoDbKeyScalarTypeDto] to a core [DynamoDbValueType].
         */
        fun fromKeyScalarDto(dto: DynamoDbKeyScalarTypeDto): DynamoDbValueType = valueOf(dto.name)
    }
}
