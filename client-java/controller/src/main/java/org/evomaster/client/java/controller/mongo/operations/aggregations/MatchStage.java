package org.evomaster.client.java.controller.mongo.operations.aggregations;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import java.util.Objects;

/**
 * Represents the $match stage.
 * It keeps the documents that satisfy a query, like WHERE (or HAVING, if it follows a $group) in SQL.
 */
public class MatchStage extends AggregationStage {

    public static final String NAME = "$match";

    private final QueryOperation filter;

    public MatchStage(QueryOperation filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    /**
     * @return the query that the documents must satisfy. It is the same kind of query as the one in a find.
     */
    public QueryOperation getFilter() {
        return filter;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
