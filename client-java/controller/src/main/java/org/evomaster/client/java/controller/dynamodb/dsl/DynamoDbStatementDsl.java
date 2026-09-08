package org.evomaster.client.java.controller.dynamodb.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;

import java.util.List;

/**
 * Fluent definition of one DynamoDB item.
 */
public interface DynamoDbStatementDsl extends DynamoDbSequenceDsl {

    /**
     * Adds a scalar attribute using its printable representation. Strings must be enclosed in single quotes,
     * numbers are represented by their exact text, and booleans are represented by {@code true} or {@code false}.
     *
     * @param attributeName attribute name
     * @param printableValue scalar value in printable form
     * @return the continuation of this statement
     */
    DynamoDbStatementDsl d(String attributeName, String printableValue);

    /** @return the completed insertion DTOs */
    List<DynamoDbInsertionDto> dtos();
}
