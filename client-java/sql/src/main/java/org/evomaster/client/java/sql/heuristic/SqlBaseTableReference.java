package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.internal.SqlTableId;

import java.util.Objects;

/**
 * Represents a reference to a base table in SQL.
 * Base tables are physical tables in the database.
 */
public class SqlBaseTableReference extends SqlTableReference {

    private final SqlTableId tableId;

    public SqlBaseTableReference(String catalog, String schema, String baseTableName) {
        Objects.requireNonNull(baseTableName);
        this.tableId = new SqlTableId(catalog, schema, baseTableName);
    }

    public SqlBaseTableReference(String baseTableName) {
        this(null, null, baseTableName);
    }

    public String getName() {
        return tableId.getTableName();
    }

    public SqlTableId getTableId() {
        return tableId;
    }

    public String toString() {
        return tableId.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlBaseTableReference) {
            SqlBaseTableReference other = (SqlBaseTableReference) obj;
            return tableId.equals(other.tableId);
        }
        return false;
    }

    public int hashCode() {
        return tableId.hashCode();
    }
}
