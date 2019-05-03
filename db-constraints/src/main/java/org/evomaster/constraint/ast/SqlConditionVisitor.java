package org.evomaster.constraint.ast;

public abstract class SqlConditionVisitor<K, V> {

    public abstract K visit(SqlAndCondition e, V argument);

    public abstract K visit(SqlBigDecimalLiteralValue e, V argument);

    public abstract K visit(SqlBigIntegerLiteralValue e, V argument);

    public abstract K visit(SqlBooleanLiteralValue e, V argument);

    public abstract K visit(SqlColumn e, V argument);

    public abstract K visit(SqlComparisonCondition e, V argument);

    public abstract K visit(SqlNullLiteralValue e, V argument);

    public abstract K visit(SqlStringLiteralValue e, V argument);

    public abstract K visit(SqlConditionList e, V argument);

    public abstract K visit(SqlInCondition e, V argument);

    public abstract K visit(SqlIsNotNullCondition e, V argument);

    public abstract K visit(SqlBinaryDataLiteralValue e, V argument);

    public abstract K visit(SqlSimilarToCondition e, V argument);

    public abstract K visit(SqlIsNullCondition e, V argument);

    public abstract K visit(SqlLikeCondition e, V argument);

    public abstract K visit(SqlOrCondition e, V argument);
}
