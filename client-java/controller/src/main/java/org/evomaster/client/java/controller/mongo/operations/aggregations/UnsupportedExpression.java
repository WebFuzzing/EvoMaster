package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents an expression that cannot be evaluated, as it uses an operator or a variable that are not supported.
 */
public class UnsupportedExpression extends AggregationExpression {

    private final String description;

    /**
     * @param description what is not supported (eg, the name of the operator)
     */
    public UnsupportedExpression(String description) {
        this.description = Objects.requireNonNull(description);
    }

    public String getDescription() {
        return description;
    }
}
