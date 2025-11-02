package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlColumn extends SqlCondition {

    private final /* nullable*/ String tableName;

    private final /* non-null*/ String columnName;

    public SqlColumn(String columnName) {
        this.tableName = null;
        this.columnName = Objects.requireNonNull(columnName);
    }

    public SqlColumn(String tableName, String columnName) {
        this.tableName = Objects.requireNonNull(tableName);
        this.columnName = Objects.requireNonNull(columnName);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlColumn that = (SqlColumn) o;
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
