package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $all operation.
 * Matches arrays that contain all elements specified in the query.
 */
public class AllOperation<V> extends QueryOperationWithField {
    private final List<V> values;

    public AllOperation(String fieldName, List<V> values) {
        super(fieldName);
        this.values = values;
    }

    public List<V> getValues() {
        return values;
    }
}
