package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class InExpression extends CheckExpr {

    private final /* non-null*/ ColumnName columnName;

    private final /* non-null*/ CheckExprList literalList;

    public InExpression(ColumnName columnName, CheckExprList literalList) {
        this.columnName = columnName;
        this.literalList = literalList;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InExpression that = (InExpression) o;
        return columnName.equals(that.columnName) &&
                literalList.equals(that.literalList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, literalList);
    }

    @Override
    public String toSql() {
        return columnName.toSql() +
                " IN " + literalList.toSql();
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public ColumnName getColumnName() {
        return columnName;
    }

    public CheckExprList getLiteralList() {
        return literalList;
    }
}
