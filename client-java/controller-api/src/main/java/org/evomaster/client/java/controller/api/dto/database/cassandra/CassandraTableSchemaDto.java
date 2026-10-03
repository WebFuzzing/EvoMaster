package org.evomaster.client.java.controller.api.dto.database.cassandra;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The shape of a Cassandra table, ie the columns a row of it is composed of, as read from the
 * driver's own metadata on the SUT side. It is what the core process bases the data it generates
 * for such a table on.
 */
public class CassandraTableSchemaDto {

    private String keyspaceName;

    private String tableName;

    /**
     * All the columns of the table, in the order the driver reports them.
     */
    private List<CassandraColumnDto> columns = new ArrayList<>();

    /**
     * Needed to deserialize the DTO, as it is sent over HTTP.
     */
    public CassandraTableSchemaDto() {
    }

    /**
     * @param keyspaceName the keyspace the table belongs to
     * @param tableName    the name of the table
     * @param columns      all the columns of the table
     * @throws NullPointerException if any of the arguments is null
     */
    public CassandraTableSchemaDto(String keyspaceName, String tableName, List<CassandraColumnDto> columns) {
        this.keyspaceName = Objects.requireNonNull(keyspaceName, "keyspaceName cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
        this.columns = Objects.requireNonNull(columns, "columns cannot be null");
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = Objects.requireNonNull(keyspaceName, "keyspaceName cannot be null");
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
    }

    public List<CassandraColumnDto> getColumns() {
        return columns;
    }

    public void setColumns(List<CassandraColumnDto> columns) {
        this.columns = Objects.requireNonNull(columns, "columns cannot be null");
    }
}