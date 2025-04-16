package org.evomaster.client.java.sql.heuristic;

import java.util.Objects;

public class ColumnReference {

    private final SqlTableReference sqlTableReference;
    private final String columnName;

    public ColumnReference(SqlTableReference sqlTableReference, String columnName) {
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
        if (obj instanceof ColumnReference) {
            ColumnReference other = (ColumnReference) obj;
            return Objects.equals(sqlTableReference, other.sqlTableReference) && Objects.equals(columnName, other.columnName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlTableReference, columnName);
    }
}
