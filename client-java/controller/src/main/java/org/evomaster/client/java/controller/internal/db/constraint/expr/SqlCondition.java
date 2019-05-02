package org.evomaster.client.java.controller.internal.db.constraint.expr;

public abstract class SqlCondition {

    public abstract String toSql();

    public abstract <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument);

    public final String toString() {
        return toSql();
    }
}
