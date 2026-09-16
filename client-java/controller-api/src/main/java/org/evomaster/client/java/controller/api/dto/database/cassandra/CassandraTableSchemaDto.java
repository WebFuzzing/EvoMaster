package org.evomaster.client.java.controller.api.dto.database.cassandra;

import java.util.ArrayList;
import java.util.List;

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

    public CassandraTableSchemaDto(String keyspaceName, String tableName, List<CassandraColumnDto> columns) {
        this.keyspaceName = keyspaceName;
        this.tableName = tableName;
        this.columns = columns;
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

    public List<CassandraColumnDto> getColumns() {
        return columns;
    }

    public void setColumns(List<CassandraColumnDto> columns) {
        this.columns = columns;
    }
}