package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of rows in a Cassandra table.
 */
public class CassandraTableSchema implements Serializable {
    private final String tableName;
    private final String tableSchema;

    public CassandraTableSchema(String tableName, String tableSchema) {
        this.tableName = tableName;
        this.tableSchema = tableSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CassandraTableSchema that = (CassandraTableSchema) o;
        return Objects.equals(tableName, that.tableName) && Objects.equals(tableSchema, that.tableSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, tableSchema);
    }

    @Override
    public String toString() {
        return "CassandraTableSchema{" +
                "tableName='" + tableName + '\'' +
                ", tableSchema='" + tableSchema + '\'' +
                '}';
    }
}