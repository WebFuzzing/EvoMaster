package org.evomaster.client.java.controller.dynamodb.operations;

import org.evomaster.client.java.controller.dynamodb.DynamoDbComparisonType;

/**
 * DynamoDB {@code size(path) comparator value} predicate operation.
 */
public class SizeOperation extends QueryOperation {

    private final String fieldName;
    private final DynamoDbComparisonType comparator;
    private final Object expectedValue;

    /**
     * Creates a size operation.
     *
     * @param fieldName attribute path
     * @param comparator comparison operator
     * @param expectedValue expected value
     */
    public SizeOperation(String fieldName, DynamoDbComparisonType comparator, Object expectedValue) {
        this.fieldName = fieldName;
        this.comparator = comparator;
        this.expectedValue = expectedValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public DynamoDbComparisonType getComparator() {
        return comparator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }
}
