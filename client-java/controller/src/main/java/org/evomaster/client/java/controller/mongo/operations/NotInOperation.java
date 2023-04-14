package org.evomaster.client.java.controller.mongo.operations;

import java.util.ArrayList;

/**
 * Represent $nin operation.
 * Selects the documents where:
 *  - the field value is not in the specified array
 *  - the field does not exist.
 */
public class NotInOperation<V> extends QueryOperation{
    private final String fieldName;
    private final ArrayList<V> values;

    public NotInOperation(String fieldName, ArrayList<V> values) {
        this.fieldName = fieldName;
        this.values = values;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ArrayList<V> getValues() {
        return values;
    }
}