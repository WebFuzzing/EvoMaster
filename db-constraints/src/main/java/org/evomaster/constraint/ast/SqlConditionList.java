package org.evomaster.constraint.ast;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlConditionList extends SqlCondition {

    private final /* non-null */ List<SqlCondition> sqlConditionExpressions;

    public SqlConditionList(List<SqlCondition> sqlConditionList) {
        super();
        if (sqlConditionList == null) {
            throw new IllegalArgumentException("Cannot create a checkExpr list with a null list");
        }
        this.sqlConditionExpressions = sqlConditionList;
    }

    @Override
    public String toSql() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(join(this.sqlConditionExpressions.stream().map(SqlCondition::toSql).collect(Collectors.toList()), ","));
        builder.append(")");
        return builder.toString();
    }


    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlConditionList that = (SqlConditionList) o;
        return sqlConditionExpressions.equals(that.sqlConditionExpressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlConditionExpressions);
    }

    public List<SqlCondition> getSqlConditionExpressions() {
        return sqlConditionExpressions;
    }
}
