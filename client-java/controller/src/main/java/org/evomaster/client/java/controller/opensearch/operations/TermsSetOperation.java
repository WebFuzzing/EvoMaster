package org.evomaster.client.java.controller.opensearch.operations;

import java.util.List;

/**
 * Represents Terms Set operation.
 * Matches documents that contain a minimum number of exact terms in a specified field.
 * Similar to Terms query but allows specifying minimum number of matching terms required.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/terms-set/">OpenSearch Terms Set Operation</a>
 */
public class TermsSetOperation<V> extends QueryOperation {
    private final String fieldName;
    private final List<V> terms;
    private final String minimumShouldMatchField;
    private final String minimumShouldMatchScript;
    private final Float boost;

    public TermsSetOperation(String fieldName, List<V> terms, String minimumShouldMatchField) {
        this(fieldName, terms, minimumShouldMatchField, null, null);
    }

    public TermsSetOperation(String fieldName, List<V> terms, String minimumShouldMatchField, 
                           String minimumShouldMatchScript, Float boost) {
        this.fieldName = fieldName;
        this.terms = terms;
        this.minimumShouldMatchField = minimumShouldMatchField;
        this.minimumShouldMatchScript = minimumShouldMatchScript;
        this.boost = boost;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<V> getTerms() {
        return terms;
    }

    public String getMinimumShouldMatchField() {
        return minimumShouldMatchField;
    }

    public String getMinimumShouldMatchScript() {
        return minimumShouldMatchScript;
    }

    public Float getBoost() {
        return boost;
    }
}
