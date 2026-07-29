package org.evomaster.client.java.controller.api.dto.database.execution;


/**
 * Describes a CQL query whose target table was found empty when the corresponding heuristic
 * distance was computed, as a hint for data-generation.
 */
public class CassandraFailedQuery {
    /**
     * The keyspace the table belongs to.
     */
    private final String keyspaceName;
    /**
     * The table the query targeted.
     */
    private final String tableName;
    /**
     * The schema of the table's rows, if known.
     */
    private String tableSchema;

    public CassandraFailedQuery(String keyspaceName, String tableName, String tableSchema) {
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
        this.tableSchema = tableSchema;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableSchema() {
        return tableSchema;
    }
}