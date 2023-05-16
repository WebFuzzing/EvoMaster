package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $all operation.
 * Matches arrays that contain all elements specified in the query.
 */
public class AllOperation<V> extends QueryOperation{
    private final String fieldName;
    private final List<V> values;

    public AllOperation(String fieldName, List<V> values) {
        this.fieldName = fieldName;
        this.values = values;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<V> getValues() {
        return values;
    }
}