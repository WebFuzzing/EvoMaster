package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class SqlInCondition extends SqlCondition {

    private final /* non-null*/ SqlColumnName sqlColumnName;

    private final /* non-null*/ SqlConditionList literalList;

    public SqlInCondition(SqlColumnName sqlColumnName, SqlConditionList literalList) {
        this.sqlColumnName = sqlColumnName;
        this.literalList = literalList;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlInCondition that = (SqlInCondition) o;
        return sqlColumnName.equals(that.sqlColumnName) &&
                literalList.equals(that.literalList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlColumnName, literalList);
    }

    @Override
    public String toSql() {
        return sqlColumnName.toSql() +
                " IN " + literalList.toSql();
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public SqlColumnName getSqlColumnName() {
        return sqlColumnName;
    }

    public SqlConditionList getLiteralList() {
        return literalList;
    }
}
