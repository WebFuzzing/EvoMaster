package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a document whose fields are expressions.
 */
public class DocumentExpression extends AggregationExpression {

    private final Map<String, AggregationExpression> fields;

    /**
     * @param fields the expression of each field. The order of the fields is the one of the map.
     */
    public DocumentExpression(Map<String, AggregationExpression> fields) {
        this.fields = Objects.requireNonNull(fields);
    }

    public Map<String, AggregationExpression> getFields() {
        return fields;
    }
}
