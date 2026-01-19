package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.schema.Table;

import java.util.Objects;

/**
 * Represents a reference to a base table in SQL.
 * Base tables are physical tables in the database.
 */
public class SqlTableName extends SqlTableReference {

    private final Table table;

    public SqlTableName(Table table) {
        Objects.requireNonNull(table);
        this.table = table;
    }

    public Table getTable() {
        return table;
    }

    public String toString() {
        return table.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlTableName) {
            SqlTableName other = (SqlTableName) obj;
            return table.equals(other.table);
        }
        return false;
    }

    public int hashCode() {
        return table.hashCode();
    }
}
