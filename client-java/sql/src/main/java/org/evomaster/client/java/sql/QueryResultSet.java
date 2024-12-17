package org.evomaster.client.java.sql;

import java.util.Map;
import java.util.TreeMap;

public class QueryResultSet {

    /**
     * A map of table names to Query results
     */
    private final Map<String, QueryResult> queryResults;
    private final boolean isCaseSensitive;
    private QueryResult queryResultForVirtualTable;

    public QueryResultSet() {
        this(true);
    }

    public QueryResultSet(boolean isCaseSensitive) {
        queryResults = new TreeMap<>(isCaseSensitive ? null : String.CASE_INSENSITIVE_ORDER);
        this.isCaseSensitive = isCaseSensitive;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

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

    public QueryResult getQueryResultForNamedTable(String tableName) {
        return queryResults.get(tableName);
    }

    public QueryResult getQueryResultForVirtualTable() {
        return queryResultForVirtualTable;
    }

}
