package org.evomaster.dbconstraint.ast;

public class SqlBinaryDataLiteralValue extends SqlLiteralValue {

    private final String hexString;

    public SqlBinaryDataLiteralValue(String hexString) {
        this.hexString = hexString;
    }


    @Override
    public String toSql() {
        return hexString;
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
