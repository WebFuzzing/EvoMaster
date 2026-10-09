package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * Represents a constant: a number, a string that is not a field path, a boolean, null...
 */
public class LiteralExpression extends AggregationExpression {

    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    /**
     * @return the value, that can be null
     */
    public Object getValue() {
        return value;
    }
}
