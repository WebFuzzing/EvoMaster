package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

public class InOperation<V> extends QueryOperation {

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
