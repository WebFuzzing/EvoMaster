package org.evomaster.client.java.controller.internal.db.constraint.expr;

public class NullLiteral extends LiteralValue {

    public NullLiteral() {
        super();
    }

    @Override
    public String toSql() {
        return "NULL";
    }


    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
