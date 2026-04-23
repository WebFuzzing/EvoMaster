package org.evomaster.client.java.controller.dynamodb.operations;

public class BetweenOperation extends QueryOperation {

    private final String fieldName;
    private final Object lowerBound;
    private final Object upperBound;

    public BetweenOperation(String fieldName, Object lowerBound, Object upperBound) {
        this.fieldName = fieldName;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getLowerBound() {
        return lowerBound;
    }

    public Object getUpperBound() {
        return upperBound;
    }
}
