package org.evomaster.client.java.controller.internal.db.constraint;


import net.sf.jsqlparser.expression.StringValue;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SchemaConstraintExtractor extends CheckExprVisitor<List<SchemaConstraint>, Void> {

    /**
     * FIXME
     * temporary workaround before major refactoring.
     * Recall that Column.getTable() is not reliable
     */
    private static String getTableName(ColumnName column) {
        String tableName = column.getTableName();
        if (tableName != null) {
            return tableName;
        }

        return "?";
    }


    @Override
    public List<SchemaConstraint> visit(AndFormula andExpression, Void argument) {
        List<SchemaConstraint> constraints = new LinkedList<>();
        constraints.addAll(andExpression.getLeftExpr().accept(this, null));
        constraints.addAll(andExpression.getRightExpr().accept(this, null));
        return constraints;
    }

    @Override
    public List<SchemaConstraint> visit(ComparisonExpr e, Void argument) {
        CheckExpr left = e.getLeftOperand();
        CheckExpr right = e.getRightOperand();

        if (left instanceof LiteralValue && right instanceof ColumnName) {
            LiteralValue leftLiteral = (LiteralValue) left;
            ColumnName rightColumn = (ColumnName) right;
            if (leftLiteral instanceof BigIntegerLiteral) {
                long value = ((BigIntegerLiteral) leftLiteral).getBigInteger().longValue();
                final String tableName = getTableName(rightColumn);
                final String columnName = rightColumn.getColumnName();
                switch (e.getComparisonOperator()) {
                    case EQUALS_TO: {
                        return Collections.singletonList(new RangeConstraint(tableName, columnName, value, value));
                    }
                    case GREATER_THAN: {
                        return Collections.singletonList(new UpperBoundConstraint(tableName, columnName, value - 1));
                    }
                    case GREATER_THAN_OR_EQUAL: {
                        return Collections.singletonList(new UpperBoundConstraint(tableName, columnName, value));
                    }
                    case LESS_THAN: {
                        return Collections.singletonList(new LowerBoundConstraint(tableName, columnName, value + 1));
                    }
                    case LESS_THAN_OR_EQUAL: {
                        return Collections.singletonList(new LowerBoundConstraint(tableName, columnName, value));
                    }
                    default: {
                        throw new RuntimeException("Unexpected comparison operator " + e.getComparisonOperator());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported literal " + e.getComparisonOperator());
            }
        } else if (left instanceof ColumnName && right instanceof LiteralValue) {
            ColumnName leftColumn = (ColumnName) left;
            LiteralValue rightLiteral = (LiteralValue) right;
            if (rightLiteral instanceof BigIntegerLiteral) {
                long value = ((BigIntegerLiteral) rightLiteral).getBigInteger().longValue();
                final String tableName = getTableName(leftColumn);
                final String columnName = leftColumn.getColumnName();
                switch (e.getComparisonOperator()) {
                    case EQUALS_TO: {
                        return Collections.singletonList(new RangeConstraint(tableName, columnName, value, value));
                    }
                    case GREATER_THAN: {
                        return Collections.singletonList(new LowerBoundConstraint(tableName, columnName, value + 1));
                    }
                    case GREATER_THAN_OR_EQUAL: {
                        return Collections.singletonList(new LowerBoundConstraint(tableName, columnName, value));
                    }
                    case LESS_THAN: {
                        return Collections.singletonList(new UpperBoundConstraint(tableName, columnName, value - 1));
                    }
                    case LESS_THAN_OR_EQUAL: {
                        return Collections.singletonList(new UpperBoundConstraint(tableName, columnName, value));
                    }
                    default: {
                        throw new RuntimeException("Unexpected comparison operator " + e.getComparisonOperator());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported literal " + e.getComparisonOperator());
            }

        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }

    }


    @Override
    public List<SchemaConstraint> visit(InExpression inExpression, Void argument) {

        ColumnName column = inExpression.getColumnName();
        String columnName = column.getColumnName();
        CheckExprList rightItemsList = inExpression.getLiteralList();

        List<String> stringValues = new LinkedList<>();
        for (CheckExpr expressionValue : rightItemsList.getCheckExpressions()) {
            final String stringValue;
            if (expressionValue instanceof StringLiteral) {
                stringValue = new StringValue(expressionValue.toSql()).getNotExcapedValue();
            } else {
                stringValue = expressionValue.toSql();
            }
            stringValues.add(stringValue);
        }
        return Collections.singletonList(new EnumConstraint(columnName, stringValues));

    }

    @Override
    public List<SchemaConstraint> visit(NullLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(StringLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(CheckExprList e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(IsNotNullExpr e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(BinaryLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(BigDecimalLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(BigIntegerLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(BooleanLiteral e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(ColumnName e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }


}
