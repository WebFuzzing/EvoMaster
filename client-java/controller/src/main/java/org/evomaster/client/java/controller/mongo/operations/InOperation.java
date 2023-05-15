package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $in operation.
 * Selects the documents where the value of a field equals any value in the specified array.
 */
public class InOperation<V> extends QueryOperation{
    private final String fieldName;
    private final List<V> values;

    public InOperation(String fieldName, List<V> values) {
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