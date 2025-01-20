package org.evomaster.client.java.sql;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a collection of query results mapped to table names,
 * with support for both named and virtual tables.
 *
 * This class allows case-sensitive or case-insensitive handling of table names
 * and provides mechanisms to add, retrieve, and manage query results.
 *
 */
public class QueryResultSet {

    /**
     * A map storing query results associated with table names.
     * The keys are table names, and the values are {@link QueryResult} objects.
     */
    private final Map<String, QueryResult> queryResults;

    /**
     * Indicates whether table name comparisons are case-sensitive.
     */
    private final boolean isCaseSensitive;

    /**
     * Stores the query result for a virtual table, if any.
     */
    private QueryResult queryResultForVirtualTable;

    public QueryResultSet() {
        this(true);
    }

    /**
     * Creates a new {@code QueryResultSet}.
     *
     * @param isCaseSensitive whether table name comparisons should be case-sensitive
     */
    public QueryResultSet(boolean isCaseSensitive) {
        queryResults = new TreeMap<>(isCaseSensitive ? null : String.CASE_INSENSITIVE_ORDER);
        this.isCaseSensitive = isCaseSensitive;
    }

    /**
     * Returns whether table name comparisons are case-sensitive.
     *
     * @return {@code true} if comparisons are case-sensitive, {@code false} otherwise
     */
    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    /**
     * Adds a query result to the result set.
     * <p>
     * If the query result corresponds to a named table, it is stored in the map.
     * If it corresponds to a virtual table, it is stored separately.
     * Throws an exception if a duplicate table (named or virtual) is added.
     * </p>
     *
     * @param queryResult the query result to add
     * @throws IllegalArgumentException if the table name already exists in the set
     */
    public void addQueryResult(QueryResult queryResult) {
        String tableName = queryResult.seeVariableDescriptors()
                .stream()
                .findFirst()
                .map(VariableDescriptor::getTableName)
                .orElse(null);

        if (tableName == null) {
            handleVirtualTable(queryResult);
        } else {
            handleNamedTable(tableName, queryResult);
        }
    }

    private void handleNamedTable(String tableName, QueryResult queryResult) {
        if (queryResults.containsKey(tableName)) {
            throw new IllegalArgumentException("Duplicate table in QueryResultSet: " + tableName);
        }
        queryResults.put(tableName, queryResult);
    }

    private void handleVirtualTable(QueryResult queryResult) {
        if (queryResultForVirtualTable != null) {
            throw new IllegalArgumentException("Duplicate values for virtual table");
        }
        queryResultForVirtualTable = queryResult;
    }

    /**
     * Retrieves the query result associated with a named table.
     *
     * @param tableName the name of the table
     * @return the query result for the table, or {@code null} if no result exists
     */
    public QueryResult getQueryResultForNamedTable(String tableName) {
        return queryResults.get(tableName);
    }

    /**
     * Retrieves the query result for a virtual table.
     *
     * @return the query result for the virtual table, or {@code null} if none exists
     */
    public QueryResult getQueryResultForVirtualTable() {
        return queryResultForVirtualTable;
    }

}
