package org.evomaster.client.java.controller.internal.db.constraint.expr;

public abstract class CheckExpr {

    public abstract String toSql();

    public abstract <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument);

    public final String toString() {
        return toSql();
    }
}
