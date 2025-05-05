package org.evomaster.client.java.sql.internal;

/**
 * A class representing a SQL column identifier
 * from a physical database table.
 *
 * This class simply is a wrapper class for the string for the column id.
 * No case sensitive is considered when comparing column ids within
 * this class.
 */
public class SqlColumnId {

    private final String columnId;

    public SqlColumnId(String columnId) {
        this.columnId = columnId;
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
            return columnId.equals(other.columnId);
        }
        return false;
    }

    public int hashCode() {
        return columnId.hashCode();
    }
}
