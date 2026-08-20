package org.evomaster.client.java.controller.dynamodb;

import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB attribute types accepted by the {@code attribute_type} expression function.
 */
public enum DynamoDbAttributeType {

    STRING("S", ValueFamily.STRING, false),
    STRING_SET("SS", ValueFamily.STRING, true),
    NUMBER("N", ValueFamily.NUMBER, false),
    NUMBER_SET("NS", ValueFamily.NUMBER, true),
    BINARY("B", ValueFamily.BINARY, false),
    BINARY_SET("BS", ValueFamily.BINARY, true),
    BOOLEAN("BOOL", ValueFamily.NONE, false),
    NULL("NULL", ValueFamily.NONE, false),
    LIST("L", ValueFamily.NONE, false),
    MAP("M", ValueFamily.NONE, false);

    private static final Map<String, DynamoDbAttributeType> BY_TOKEN = new HashMap<>();

    static {
        for (DynamoDbAttributeType type : values()) {
            BY_TOKEN.put(type.token, type);
        }
    }

    private final String token;
    private final ValueFamily family;
    private final boolean set;

    /**
     * Creates a DynamoDB attribute type.
     *
     * @param token DynamoDB type token
     * @param family value family used to relate scalar and set types
     * @param set whether this is the set variant of its value family
     */
    DynamoDbAttributeType(String token, ValueFamily family, boolean set) {
        this.token = token;
        this.family = family;
        this.set = set;
    }

    /**
     * Maps an exact DynamoDB type token to its enum value.
     *
     * @param token DynamoDB attribute type token
     * @return matching attribute type
     * @throws IllegalArgumentException when the token is null or unsupported
     */
    public static DynamoDbAttributeType fromToken(String token) {
        DynamoDbAttributeType type = BY_TOKEN.get(token);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + token);
        }
        return type;
    }

    /**
     * @return DynamoDB type token used in expression attribute values
     */
    public String getToken() {
        return token;
    }

    /**
     * Checks whether this type and another type are scalar/set variants of the same value family.
     *
     * @param other type to compare
     * @return {@code true} for the S/SS, N/NS, and B/BS pairs in either direction
     */
    public boolean isScalarSetVariantOf(DynamoDbAttributeType other) {
        return other != null
                && family != ValueFamily.NONE
                && family == other.family
                && set != other.set;
    }

    /** Value family used to identify scalar and set variants. */
    private enum ValueFamily {
        STRING,
        NUMBER,
        BINARY,
        NONE
    }
}
