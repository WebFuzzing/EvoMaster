package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.HashMap;
import java.util.Map;

/**
 * Native DynamoDB value types supported by insertion attributes.
 * Binary ({@code B}) and binary sets ({@code BS}) are intentionally unsupported.
 */
public enum DynamoDbValueTypeDto {
    STRING("S"),
    NUMBER("N"),
    BOOLEAN("BOOL"),
    MAP("M"),
    LIST("L"),
    STRING_SET("SS"),
    NUMBER_SET("NS"),
    NULL("NULL");

    /** Wire type code for unsupported DynamoDB binary ({@code B}) values. */
    public static final String BINARY_WIRE_CODE = "B";

    /** Wire type code for unsupported DynamoDB binary set ({@code BS}) values. */
    public static final String BINARY_SET_WIRE_CODE = "BS";

    private static final Map<String, DynamoDbValueTypeDto> BY_WIRE_CODE = new HashMap<>();

    static {
        for (DynamoDbValueTypeDto type : values()) {
            BY_WIRE_CODE.put(type.wireCode, type);
        }
    }

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

    /**
     * Maps a DynamoDB wire type code to its supported {@link DynamoDbValueTypeDto}.
     * Binary ({@code B}) and binary sets ({@code BS}) are intentionally rejected.
     *
     * @param wireCode native DynamoDB wire type code
     * @return matching value type
     * @throws IllegalArgumentException if {@code wireCode} is {@code B}, {@code BS}, null, or unknown
     */
    public static DynamoDbValueTypeDto fromWireCode(String wireCode) {
        if (BINARY_WIRE_CODE.equals(wireCode)) {
            throw new IllegalArgumentException("Unsupported DynamoDB binary (B) value");
        }
        if (BINARY_SET_WIRE_CODE.equals(wireCode)) {
            throw new IllegalArgumentException("Unsupported DynamoDB binary set (BS) value");
        }
        DynamoDbValueTypeDto type = BY_WIRE_CODE.get(wireCode);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported DynamoDB wire type: " + wireCode);
        }
        return type;
    }
}
