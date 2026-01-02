package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represent Term operation.
 * Matches documents where the value of a field equals the specified value.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/term/">OpenSearch Term Operation</a>
 */
public class TermOperation<V> extends ComparisonOperation<V> {
    private final Boolean caseInsensitive;
    private final Float boost;
    private final String name;

    public TermOperation(String fieldName, V value) {
        this(fieldName, value, Boolean.FALSE, null, null);
    }

    public TermOperation(String fieldName, V value, Boolean caseInsensitive) {
        this(fieldName, value, caseInsensitive, null, null);
    }

    public TermOperation(String fieldName, V value, Boolean caseInsensitive, Float boost, String name) {
        super(fieldName, value);
        this.caseInsensitive = caseInsensitive;
        this.boost = boost;
        this.name = name;
    }

    public Boolean getCaseInsensitive() {
        return caseInsensitive;
    }

    public Float getBoost() {
        return boost;
    }

    public String getName() {
        return name;
    }
}