package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;

import java.util.List;

/**
 * Fluent definition of one DynamoDB item.
 */
public interface DynamoDbStatementDsl extends DynamoDbSequenceDsl {

    /** Adds a string attribute. */
    DynamoDbStatementDsl s(String name, String value);

    /** Adds a number attribute while preserving its exact text. */
    DynamoDbStatementDsl n(String name, String value);

    /** Adds a boolean attribute. */
    DynamoDbStatementDsl bool(String name, boolean value);

    /** @return the completed insertion DTOs */
    List<DynamoDbInsertionDto> dtos();
}
