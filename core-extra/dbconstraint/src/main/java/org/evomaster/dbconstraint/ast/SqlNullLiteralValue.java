package org.evomaster.dbconstraint.ast;

public class SqlNullLiteralValue extends SqlLiteralValue {

    public SqlNullLiteralValue() {
        super();
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else {
            return getClass().equals(o.getClass());
        }
    }

    @Override
    public String toSql() {
        return "NULL";
    }


    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }


}
