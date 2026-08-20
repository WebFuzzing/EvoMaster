package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.comparison.ComparisonOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.LessThanEqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.LessThanOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.NotEqualsOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared normalized comparison type used by DynamoDB parsers and heuristics.
 */
public enum DynamoDbComparisonType {

    EQUALS("=") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new EqualsOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return NOT_EQUALS;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof EqualsOperation<?>;
        }
    },
    NOT_EQUALS("<>") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new NotEqualsOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return EQUALS;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof NotEqualsOperation<?>;
        }
    },
    GREATER_THAN(">") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new GreaterThanOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return LESS_THAN_EQUALS;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof GreaterThanOperation<?>;
        }
    },
    GREATER_THAN_EQUALS(">=") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new GreaterThanEqualsOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return LESS_THAN;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof GreaterThanEqualsOperation<?>;
        }
    },
    LESS_THAN("<") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new LessThanOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return GREATER_THAN_EQUALS;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof LessThanOperation<?>;
        }
    },
    LESS_THAN_EQUALS("<=") {
        @Override
        public ComparisonOperation<?> toOperation(String fieldName, Object value) {
            return new LessThanEqualsOperation<>(fieldName, value);
        }

        @Override
        public DynamoDbComparisonType invert() {
            return GREATER_THAN;
        }

        @Override
        protected boolean matchesOperation(ComparisonOperation<?> operation) {
            return operation instanceof LessThanEqualsOperation<?>;
        }
    };

    private static final Map<String, DynamoDbComparisonType> BY_TOKEN = new HashMap<>();

    static {
        for (DynamoDbComparisonType type : values()) {
            BY_TOKEN.put(type.token, type);
        }
    }

    private final String token;

    DynamoDbComparisonType(String token) {
        this.token = token;
    }

    /**
     * Maps a DynamoDB comparator token to a normalized comparison type.
     *
     * @param token comparator token from parsed expression
     * @return normalized comparison type
     */
    public static DynamoDbComparisonType fromToken(String token) {
        DynamoDbComparisonType type = BY_TOKEN.get(token);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported comparator token: " + token);
        }
        return type;
    }

    /**
     * Creates a comparison operation instance for this comparison type.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     * @return concrete comparison operation
     */
    public abstract ComparisonOperation<?> toOperation(String fieldName, Object value);

    /**
     * Maps a comparison operation back to its normalized comparison type.
     *
     * @param operation comparison operation instance
     * @return associated comparison type
     */
    public static DynamoDbComparisonType fromOperation(ComparisonOperation<?> operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        for (DynamoDbComparisonType type : values()) {
            if (type.matchesOperation(operation)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported comparison operation: " + operation.getClass().getName());
    }

    /**
     * Returns the inverse of the current comparator.
     *
     * @return inverse comparison type
     */
    public abstract DynamoDbComparisonType invert();

    /**
     * Checks whether the given operation corresponds to this normalized comparison type.
     *
     * @param operation comparison operation instance
     * @return whether the operation belongs to this comparison type
     */
    protected abstract boolean matchesOperation(ComparisonOperation<?> operation);
}
