package org.evomaster.client.java.controller.opensearch.operations;

import java.util.List;

/**
 * Represents Terms operation.
 * Matches documents where the value of a field matches any of the specified terms.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/terms/">OpenSearch Terms Operation</a>
 */
public class TermsOperation<V> extends QueryOperation {
    private final String fieldName;
    private final List<V> values;
    private final Float boost;
    private final String name;
    private final String valueType;

    public TermsOperation(String fieldName, List<V> values) {
        this(fieldName, values, null, null, null);
    }

    public TermsOperation(String fieldName, List<V> values, Float boost, String name, String valueType) {
        this.fieldName = fieldName;
        this.values = values;
        this.boost = boost;
        this.name = name;
        this.valueType = valueType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<V> getValues() {
        return values;
    }

    public Float getBoost() {
        return boost;
    }

    public String getName() {
        return name;
    }

    public String getValueType() {
        return valueType;
    }
}
