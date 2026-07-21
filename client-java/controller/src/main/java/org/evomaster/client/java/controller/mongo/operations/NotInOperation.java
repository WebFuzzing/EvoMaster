package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;

/**
 * Represent $nin operation.
 * Selects the documents where:
 * - the field value is not in the specified array
 * - the field does not exist.
 */
public class NotInOperation<V> extends QueryOperationWithField {
    private final List<V> values;

    public NotInOperation(String fieldName, List<V> values) {
        super(fieldName);
        this.values = values;
    }

    public List<V> getValues() {
        return values;
    }
}
