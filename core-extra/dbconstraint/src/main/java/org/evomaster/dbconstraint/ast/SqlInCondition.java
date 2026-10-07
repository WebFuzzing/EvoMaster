package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlInCondition extends SqlCondition {

    private final /* non-null*/ SqlColumn sqlColumn;

    private final /* non-null*/ SqlConditionList literalList;

    /**
     * True for {@code NOT IN}: the column must differ from every value in the list.
     */
    private final boolean negated;

    public SqlInCondition(SqlColumn sqlColumn, SqlConditionList literalList) {
        this(sqlColumn, literalList, false);
    }

    public SqlInCondition(SqlColumn sqlColumn, SqlConditionList literalList, boolean negated) {
        this.sqlColumn = Objects.requireNonNull(sqlColumn);
        this.literalList = Objects.requireNonNull(literalList);
        this.negated = negated;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlInCondition that = (SqlInCondition) o;
        return negated == that.negated &&
                sqlColumn.equals(that.sqlColumn) &&
                literalList.equals(that.literalList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlColumn, literalList, negated);
    }

    @Override
    public String toSql() {
        return sqlColumn.toSql() +
                (negated ? " NOT IN " : " IN ") + literalList.toSql();
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public SqlColumn getSqlColumn() {
        return sqlColumn;
    }

    public SqlConditionList getLiteralList() {
        return literalList;
    }

    public boolean isNegated() {
        return negated;
    }
}
