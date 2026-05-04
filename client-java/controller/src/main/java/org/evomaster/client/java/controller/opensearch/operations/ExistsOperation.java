package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Exists operation.
 * Searches for documents that contain a specific field.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/exists/">OpenSearch Exists Operation</a>
 */
public class ExistsOperation extends QueryOperation {
    private final String field;
    private final Float boost;

    public ExistsOperation(String field) {
        this(field, null);
    }

    public ExistsOperation(String field, Float boost) {
        this.field = field;
        this.boost = boost;
    }

    public String getField() {
        return field;
    }

    public Float getBoost() {
        return boost;
    }
}
