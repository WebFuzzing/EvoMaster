package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents an array of expressions.
 */
public class ArrayExpression extends AggregationExpression {

    private final List<AggregationExpression> elements;

    public ArrayExpression(List<AggregationExpression> elements) {
        this.elements = Objects.requireNonNull(elements);
    }

    public List<AggregationExpression> getElements() {
        return elements;
    }
}
