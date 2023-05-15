package org.evomaster.client.java.controller.mongo.operations.synthetic;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import java.util.List;

/**
 * Represent the operation that results from applying a $not to an $all operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $all operation.
 */
public class InvertedAllOperation<V> extends QueryOperation{
    private final String fieldName;
    private final List<V> values;

    public InvertedAllOperation(String fieldName, List<V> values) {
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