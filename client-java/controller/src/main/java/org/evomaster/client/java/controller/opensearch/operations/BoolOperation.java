package org.evomaster.client.java.controller.opensearch.operations;

import java.util.List;

/**
 * Represents Bool operation.
 * A Boolean query can combine several query clauses into one advanced query. The clauses are combined
 * with Boolean logic to find matching documents returned in the results.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/compound/bool/">OpenSearch Bool Operation</a>
 */
public class BoolOperation extends QueryOperation {
    private final List<QueryOperation> must;
    private final List<QueryOperation> mustNot;
    private final List<QueryOperation> should;
    private final List<QueryOperation> filter;
    private final Integer minimumShouldMatch;
    private final Float boost;

    public BoolOperation(List<QueryOperation> must, List<QueryOperation> mustNot, 
                        List<QueryOperation> should, List<QueryOperation> filter) {
        this(must, mustNot, should, filter, null, null);
    }

    public BoolOperation(List<QueryOperation> must, List<QueryOperation> mustNot, 
                        List<QueryOperation> should, List<QueryOperation> filter,
                        Integer minimumShouldMatch, Float boost) {
        this.must = must;
        this.mustNot = mustNot;
        this.should = should;
        this.filter = filter;
        this.minimumShouldMatch = minimumShouldMatch;
        this.boost = boost;
    }

    public List<QueryOperation> getMust() {
        return must;
    }

    public List<QueryOperation> getMustNot() {
        return mustNot;
    }

    public List<QueryOperation> getShould() {
        return should;
    }

    public List<QueryOperation> getFilter() {
        return filter;
    }

    public Integer getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    public Float getBoost() {
        return boost;
    }
}
