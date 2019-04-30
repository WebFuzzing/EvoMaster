package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class IsNotNullExpr extends CheckExpr {
    private final /*non-null*/ ColumnName columnName;

    public IsNotNullExpr(ColumnName columnName) {
        if (columnName == null) {
            throw new IllegalArgumentException("Column name cannot be null");
        }
        this.columnName = columnName;
    }


    @Override
    public String toSql() {
        return columnName.toSql() + " IS NOT NULL";
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsNotNullExpr that = (IsNotNullExpr) o;
        return columnName.equals(that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName);
    }
}
