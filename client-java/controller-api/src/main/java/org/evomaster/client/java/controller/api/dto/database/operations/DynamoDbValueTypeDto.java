package org.evomaster.client.java.controller.api.dto.database.operations;

/**
 * Native DynamoDB value types supported by insertion attributes.
 */
public enum DynamoDbValueTypeDto {
    STRING("S"),
    NUMBER("N"),
    BOOLEAN("BOOL"),
    MAP("M"),
    LIST("L"),
    NULL("NULL");

    /** Native DynamoDB wire type code. */
    public final String wireCode;

    DynamoDbValueTypeDto(String wireCode) {
        this.wireCode = wireCode;
    }

    /**
     * Converts key scalar schema metadata to an attribute value type.
     *
     * @param scalar supported key scalar type
     * @return corresponding value type
     */
    public static DynamoDbValueTypeDto fromKeyScalar(DynamoDbKeyScalarTypeDto scalar) {
        return scalar == null ? null : valueOf(scalar.name());
    }
}
