package org.evomaster.client.java.controller.internal.db.constraint.expr;

public abstract class CheckExprVisitor<K, V> {

    public abstract K visit(AndFormula e, V argument);

    public abstract K visit(BigDecimalLiteral e, V argument);

    public abstract K visit(BigIntegerLiteral e, V argument);

    public abstract K visit(BooleanLiteral e, V argument);

    public abstract K visit(ColumnName e, V argument);

    public abstract K visit(ComparisonExpr e, V argument);

    public abstract K visit(NullLiteral e, V argument);

    public abstract K visit(StringLiteral e, V argument);

    public abstract K visit(CheckExprList e, V argument);

    public abstract K visit(InExpression e, V argument);

    public abstract K visit(IsNotNullExpr e, V argument);

    public abstract K visit(BinaryLiteral e, V argument);

}
