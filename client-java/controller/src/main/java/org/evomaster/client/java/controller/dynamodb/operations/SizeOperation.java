package org.evomaster.client.java.controller.dynamodb.operations;

import org.evomaster.client.java.controller.dynamodb.DynamoDbComparisonType;

public class SizeOperation extends QueryOperation {

    private final String fieldName;
    private final DynamoDbComparisonType comparator;
    private final Object expectedValue;

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
