package org.evomaster.dbconstraint.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlOrCondition extends SqlCondition {

    private final /*non-null*/ List<SqlCondition> conditions;

    public SqlOrCondition(SqlCondition... conditions) {
        Objects.requireNonNull(conditions);
        if (conditions.length < 2) {
            throw new IllegalArgumentException("Cannot create Or condition with a single condition");
        }
        if (Arrays.stream(conditions).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Cannot create Or condition with null conditions");
        }
        this.conditions = Arrays.asList(conditions);
    }

    public List<SqlCondition> getOrConditions() {
        return conditions;
    }

    @Override
    public String toSql() {
        return join(conditions.stream().map(SqlCondition::toSql).collect(Collectors.toList()), " OR ");
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlOrCondition that = (SqlOrCondition) o;
        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions);
    }


}
