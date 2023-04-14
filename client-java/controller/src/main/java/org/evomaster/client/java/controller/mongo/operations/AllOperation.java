package org.evomaster.client.java.controller.mongo.operations;

import java.util.ArrayList;

/**
 * Represent $all operation.
 * Matches arrays that contain all elements specified in the query.
 */
public class AllOperation<V> extends QueryOperation{
    private final String fieldName;
    private final ArrayList<V> values;

    public AllOperation(String fieldName, ArrayList<V> values) {
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