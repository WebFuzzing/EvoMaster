package org.evomaster.client.java.sql.internal;

import java.util.Objects;

/**
 * A class representing a SQL table identifier
 * from a physical database table.
 * This class simply is a wrapper class for the string for the table id.
 *
 * No case sensitivity is considered when comparing this class.
 */
public class SqlTableId implements Comparable<SqlTableId> {

    private final String tableId;

    public SqlTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableId() {
        return tableId;
    }

    public String toString() {
        return tableId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlTableId) {
            SqlTableId other = (SqlTableId) obj;
            return tableId.equals(other.tableId);
        }
        return false;
    }

    public int hashCode() {
        return tableId.hashCode();
    }

    @Override
    public int compareTo(SqlTableId o) {
        Objects.requireNonNull(o);
        return this.getTableId().compareTo(o.getTableId());
    }
}
