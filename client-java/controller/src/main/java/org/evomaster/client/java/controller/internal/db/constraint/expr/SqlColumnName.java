package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class SqlColumnName extends SqlCondition {

    private final /* nullable*/ String tableName;

    private final /* non-null*/ String columnName;

    public SqlColumnName(String columnName) {
        if (columnName == null) {
            throw new IllegalArgumentException("This value cannot be null");
        }
        this.tableName = null;
        this.columnName = columnName;
    }

    public SqlColumnName(String tableName, String columnName) {
        if (tableName == null) {
            throw new IllegalArgumentException("Invalid table name null");
        }
        if (columnName == null) {
            throw new IllegalArgumentException("This value cannot be null");
        }
        this.tableName = tableName;
        this.columnName = columnName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlColumnName that = (SqlColumnName) o;
        return Objects.equals(tableName, that.tableName) &&
                columnName.equals(that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, columnName);
    }

    @Override
    public String toSql() {
        if (tableName != null) {
            return String.format("%s.%s", tableName, columnName);
        }
        return columnName;
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }
}
