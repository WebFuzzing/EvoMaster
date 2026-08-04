package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $in operation.
 * Selects the documents where the value of a field equals any value in the specified array.
 */
public class InOperation<V> extends QueryOperationWithField {
    private final List<V> values;

    public InOperation(String fieldName, List<V> values) {
        super(fieldName);
        this.values = values;
    }

    public List<V> getValues() {
        return values;
    }
}
