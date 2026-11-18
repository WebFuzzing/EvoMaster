package org.evomaster.client.java.controller.api.dto.database.operations;

/**
 * Scalar DynamoDB attribute types supported by key metadata and equality synthesis.
 */
public enum DynamoDbKeyScalarTypeDto {
    STRING("S"),
    NUMBER("N"),
    BOOLEAN("BOOL");

    /** Native DynamoDB wire type code. */
    public final String wireCode;

    DynamoDbKeyScalarTypeDto(String wireCode) {
        this.wireCode = wireCode;
    }
}
