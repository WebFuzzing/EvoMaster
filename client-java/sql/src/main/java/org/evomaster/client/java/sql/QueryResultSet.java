package org.evomaster.client.java.sql;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents a collection of query results mapped to table names,
 * with support for both named and virtual tables.
 * <p>
 * This class only allows a case-insensitive handling of table names
 * and provides mechanisms to add, retrieve, and manage query results.
 */
public class QueryResultSet {

    /**
     * A map storing query results associated with table names.
     * The keys are table names, and the values are {@link QueryResult} objects.
     */
    private final Map<String, QueryResult> queryResults;

    /**
     * Stores the query result for a virtual table, if any.
     */
    private QueryResult queryResultForVirtualTable;

    /**
     * Creates a QueryResult set with default case sensitivity (case-insensitive).
     */
    public QueryResultSet() {
        queryResults = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    private static boolean hasMultipleTableNames(QueryResult queryResult) {
        long distinctTableCount = queryResult.seeVariableDescriptors()
                .stream()
                .map(VariableDescriptor::getTableName)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return distinctTableCount > 1;
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
        // check that are variable descriptors are from the same table
        if (hasMultipleTableNames(queryResult)) {
            throw new IllegalArgumentException("Cannot add a query result with multiple table names");
        }

        // get the table name from the first variable descriptor (all are equal)
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
        Objects.requireNonNull(tableName);

        final String lowerCaseTableName = tableName.toLowerCase();
        if (queryResults.containsKey(lowerCaseTableName)) {
            throw new IllegalArgumentException("Duplicate table in QueryResultSet: " + tableName);
        }
        queryResults.put(lowerCaseTableName, queryResult);
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
        return queryResults.get(tableName != null ? tableName.toLowerCase() : null);
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
