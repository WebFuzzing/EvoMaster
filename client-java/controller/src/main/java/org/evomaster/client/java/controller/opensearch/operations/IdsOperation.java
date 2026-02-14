package org.evomaster.client.java.controller.opensearch.operations;

import java.util.List;

/**
 * Represents IDs operation.
 * Searches for documents with one or more specific document ID values in the _id field.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/ids/">OpenSearch IDs Operation</a>
 */
public class IdsOperation extends QueryOperation {
    private final List<String> values;
    private final Float boost;

    public IdsOperation(List<String> values) {
        this(values, null);
    }

    public IdsOperation(List<String> values, Float boost) {
        this.values = values;
        this.boost = boost;
    }

    public List<String> getValues() {
        return values;
    }

    public Float getBoost() {
        return boost;
    }
}
