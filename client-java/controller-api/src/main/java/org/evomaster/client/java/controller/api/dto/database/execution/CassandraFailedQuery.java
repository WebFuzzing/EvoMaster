package org.evomaster.client.java.controller.api.dto.database.execution;

import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraTableSchemaDto;

import java.util.Objects;

/**
 * Describes a CQL query whose target table was found empty when the corresponding heuristic
 * distance was computed, as a hint for data-generation.
 */
public class CassandraFailedQuery {
    /**
     * The keyspace the table belongs to.
     */
    private String keyspaceName;
    /**
     * The table the query targeted.
     */
    private String tableName;
    /**
     * The shape of the table's rows, null when it was never captured, ie when no query referencing
     * the table was intercepted while the schema of its tables was being tracked.
     */
    private CassandraTableSchemaDto tableSchema;

    /**
     * Needed to deserialize the DTO, as it is sent over HTTP.
     */
    public CassandraFailedQuery() {
    }

    public CassandraFailedQuery(String keyspaceName, String tableName, CassandraTableSchemaDto tableSchema) {
        this.keyspaceName = Objects.requireNonNull(keyspaceName);
        this.tableName = Objects.requireNonNull(tableName);
        this.tableSchema = tableSchema;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public CassandraTableSchemaDto getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(CassandraTableSchemaDto tableSchema) {
        this.tableSchema = tableSchema;
    }
}