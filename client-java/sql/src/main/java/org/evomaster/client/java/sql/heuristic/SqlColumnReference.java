package org.evomaster.client.java.sql.heuristic;

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
 */
public class SqlColumnReference {

    private final SqlTableReference sqlTableReference;
    private final String columnName;

    public SqlColumnReference(SqlTableReference sqlTableReference, String columnName) {
        this.sqlTableReference = sqlTableReference;
        this.columnName = columnName;
    }

    public SqlTableReference getTableReference() {
        return sqlTableReference;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public String toString() {
        return sqlTableReference + "." + columnName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SqlColumnReference) {
            SqlColumnReference other = (SqlColumnReference) obj;
            return Objects.equals(sqlTableReference, other.sqlTableReference) && Objects.equals(columnName, other.columnName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlTableReference, columnName);
    }
}
