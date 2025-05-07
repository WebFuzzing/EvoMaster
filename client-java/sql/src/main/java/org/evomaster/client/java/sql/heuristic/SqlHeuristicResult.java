package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.QueryResult;

import java.util.Objects;

/**
 * Represents the result of a SQL heuristic evaluation.
 */
public class SqlHeuristicResult {

    /** The truthness value of the heuristic evaluation for a SQL query */
    private final Truthness truthness;


    /** The result of the SQL query. */
    private final QueryResult queryResult;

    /**
     * Constructs a new SqlHeuristicResult with the specified truthness and query result.
     *
     * @param truthness the truthness value of the heuristic evaluation
     * @param queryResult the result of the SQL query
     */
    public SqlHeuristicResult(Truthness truthness, QueryResult queryResult) {
        Objects.requireNonNull(truthness, "Truthness must not be null");
        Objects.requireNonNull(queryResult, "Query result must not be null");

        this.truthness = truthness;
        this.queryResult = queryResult;
    }

    /**
     * Returns the truthness value of the heuristic evaluation.
     *
     * @return the truthness value of the heuristic evaluation
     */
    public Truthness getTruthness() {
        return truthness;
    }

    /**
     * Returns the result of the SQL query.
     *
     * @return the result of the SQL query
     */
    public QueryResult getQueryResult() {
        return queryResult;
    }
}
