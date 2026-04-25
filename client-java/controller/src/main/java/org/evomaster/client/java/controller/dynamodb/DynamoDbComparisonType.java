package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.comparison.*;

/**
 * Shared normalized comparison types used by DynamoDB parsers.
 */
public enum DynamoDbComparisonType {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_EQUALS,
    LESS_THAN,
    LESS_THAN_EQUALS;

    /**
     * Maps a DynamoDB comparator token to a normalized comparison type.
     *
     * @param token comparator token from parsed expression
     * @return normalized comparison type
     */
    public static DynamoDbComparisonType fromToken(String token) {
        if ("=".equals(token)) {
            return EQUALS;
        }
        if ("<>".equals(token)) {
            return NOT_EQUALS;
        }
        if (">".equals(token)) {
            return GREATER_THAN;
        }
        if (">=".equals(token)) {
            return GREATER_THAN_EQUALS;
        }
        if ("<".equals(token)) {
            return LESS_THAN;
        }
        if ("<=".equals(token)) {
            return LESS_THAN_EQUALS;
        }
        throw new IllegalArgumentException("Unsupported comparator token: " + token);
    }

    /**
     * Creates a comparison operation instance for this comparison type.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     * @return concrete comparison operation
     */
    public ComparisonOperation<?> toOperation(String fieldName, Object value) {
        switch (this) {
            case EQUALS:
                return new EqualsOperation<>(fieldName, value);
            case NOT_EQUALS:
                return new NotEqualsOperation<>(fieldName, value);
            case GREATER_THAN:
                return new GreaterThanOperation<>(fieldName, value);
            case GREATER_THAN_EQUALS:
                return new GreaterThanEqualsOperation<>(fieldName, value);
            case LESS_THAN:
                return new LessThanOperation<>(fieldName, value);
            case LESS_THAN_EQUALS:
                return new LessThanEqualsOperation<>(fieldName, value);
            default:
                throw new IllegalStateException("Unsupported comparator: " + this);
        }
    }
}
