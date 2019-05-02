package org.evomaster.client.java.controller.internal.db.constraint.expr;

public class BinaryLiteral extends LiteralValue {

    private final String hexString;

    public BinaryLiteral(String hexString) {
        this.hexString = hexString;
    }


    @Override
    public String toSql() {
        return hexString;
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
