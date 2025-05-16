package org.evomaster.client.java.sql.internal;

import java.util.Objects;

/**
 * A class representing a SQL column identifier
 * from a physical database table.
 *
 * This class simply is a wrapper class for the string for the column id.
 * No case sensitive is considered when comparing column ids within
 * this class.
 */
public class SqlColumnId implements  Comparable<SqlColumnId>  {

    private final String columnId;

    public SqlColumnId(String columnId) {
        Objects.requireNonNull(columnId);
        this.columnId = columnId.toLowerCase();
    }

    public String getColumnId() {
        return columnId;
    }

    public String toString() {
        return columnId;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlColumnId) {
            SqlColumnId other = (SqlColumnId) obj;
            return columnId.equalsIgnoreCase(other.columnId);
        }
        return false;
    }

    public int hashCode() {
        return columnId.hashCode();
    }
    @Override
    public int compareTo(SqlColumnId o) {
        Objects.requireNonNull(o);
        return this.getColumnId().compareTo(o.getColumnId());
    }
}
