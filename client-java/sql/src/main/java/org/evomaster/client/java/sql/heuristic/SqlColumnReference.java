package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.internal.SqlColumnId;

import java.util.Objects;

/**
 * Represents a reference to a specific column in a SQL table or derived table.
 * This class encapsulates the table reference and the column name, providing
 * methods to access these details and ensuring proper equality and hash code
 * implementation.
 *
 * <p>Instances of this class are immutable and can be used to uniquely identify
 * a column within a SQL query context.</p>
 *
 * Column names are case-insensitive.
 */
public class SqlColumnReference {

    private final SqlTableReference sqlTableReference;
    private final SqlColumnId columnId;

    public SqlColumnReference(SqlTableReference sqlTableReference, String columnName) {
        Objects.requireNonNull(columnName, "Column name must not be null");

        this.sqlTableReference = sqlTableReference;
        this.columnId = new SqlColumnId(columnName);
    }

    public SqlTableReference getTableReference() {
        return sqlTableReference;
    }

    public String getColumnName() {
        return columnId.getColumnId() ;
    }

    @Override
    public String toString() {
        return sqlTableReference + "." + columnId.getColumnId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlColumnReference) {
            SqlColumnReference other = (SqlColumnReference) obj;
            return Objects.equals(sqlTableReference, other.sqlTableReference) && Objects.equals(columnId, other.columnId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlTableReference, columnId);
    }
}
