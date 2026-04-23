package org.evomaster.dbconstraint.ast;

public interface SqlConditionVisitor<K, V> {

    K visit(SqlAndCondition e, V argument);

    K visit(SqlBigDecimalLiteralValue e, V argument);

    K visit(SqlBigIntegerLiteralValue e, V argument);

    K visit(SqlBooleanLiteralValue e, V argument);

    K visit(SqlColumn e, V argument);

    K visit(SqlComparisonCondition e, V argument);

    K visit(SqlNullLiteralValue e, V argument);

    K visit(SqlStringLiteralValue e, V argument);

    K visit(SqlConditionList e, V argument);

    K visit(SqlInCondition e, V argument);

    K visit(SqlIsNotNullCondition e, V argument);

    K visit(SqlBinaryDataLiteralValue e, V argument);

    K visit(SqlSimilarToCondition e, V argument);

    K visit(SqlIsNullCondition e, V argument);

    K visit(SqlLikeCondition e, V argument);

    K visit(SqlOrCondition e, V argument);
}
