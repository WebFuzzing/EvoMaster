package org.evomaster.client.java.instrumentation.cassandra;

import java.util.Objects;

/**
 * Identifies a Cassandra table by its resolved (never null) keyspace and table name, in the
 * driver's canonical/internal form. Used as a map key wherever per-table state is cached, since
 * table names aren't unique across keyspaces.
 */
public class TableKey {

    /**
     * The keyspace a table belongs to.
     */
    private final String keyspaceName;
    /**
     * The table name.
     */
    private final String tableName;

    public TableKey(String keyspaceName, String tableName) {
        this.keyspaceName = Objects.requireNonNull(keyspaceName);
        this.tableName = Objects.requireNonNull(tableName);
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableKey)) return false;
        TableKey tableKey = (TableKey) o;
        return Objects.equals(keyspaceName, tableKey.keyspaceName) && Objects.equals(tableName, tableKey.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyspaceName, tableName);
    }
}