package org.evomaster.client.java.controller.internal.db.constraint.expr;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CheckExprList extends CheckExpr {

    private final /* non-null */ List<CheckExpr> checkExpressions;

    public CheckExprList(List<CheckExpr> checkExprList) {
        super();
        if (checkExprList == null) {
            throw new IllegalArgumentException("Cannot create a checkExpr list with a null list");
        }
        this.checkExpressions = checkExprList;
    }

    @Override
    public String toSql() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(StringUtils.join(this.checkExpressions.stream().map(CheckExpr::toSql).collect(Collectors.toList()), ","));
        builder.append(")");
        return builder.toString();
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckExprList that = (CheckExprList) o;
        return checkExpressions.equals(that.checkExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkExpressions);
    }
}
