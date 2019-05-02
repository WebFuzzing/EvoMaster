package org.evomaster.client.java.controller.internal.db.constraint;


import net.sf.jsqlparser.expression.StringValue;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SchemaConstraintExtractor extends SqlConditionVisitor<List<SchemaConstraint>, Void> {

    /**
     * FIXME
     * temporary workaround before major refactoring.
     * Recall that Column.getTable() is not reliable
     */
    private static String getTableName(SqlColumnName column) {
        String tableName = column.getTableName();
        if (tableName != null) {
            return tableName;
        }

        return "?";
    }


    @Override
    public List<SchemaConstraint> visit(SqlAndCondition andExpression, Void argument) {
        List<SchemaConstraint> constraints = new LinkedList<>();
        constraints.addAll(andExpression.getLeftExpr().accept(this, null));
        constraints.addAll(andExpression.getRightExpr().accept(this, null));
        return constraints;
    }

    @Override
    public List<SchemaConstraint> visit(SqlComparisonCondition e, Void argument) {
        SqlCondition left = e.getLeftOperand();
        SqlCondition right = e.getRightOperand();

        if (left instanceof SqlLiteralValue && right instanceof SqlColumnName) {
            SqlLiteralValue leftLiteral = (SqlLiteralValue) left;
            SqlColumnName rightColumn = (SqlColumnName) right;
            if (leftLiteral instanceof SqlBigIntegerLiteralValue) {
                long value = ((SqlBigIntegerLiteralValue) leftLiteral).getBigInteger().longValue();
                final String tableName = getTableName(rightColumn);
                final String columnName = rightColumn.getColumnName();
                switch (e.getSqlComparisonOperator()) {
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
                        throw new RuntimeException("Unexpected comparison operator " + e.getSqlComparisonOperator());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported literal " + e.getSqlComparisonOperator());
            }
        } else if (left instanceof SqlColumnName && right instanceof SqlLiteralValue) {
            SqlColumnName leftColumn = (SqlColumnName) left;
            SqlLiteralValue rightLiteral = (SqlLiteralValue) right;
            if (rightLiteral instanceof SqlBigIntegerLiteralValue) {
                long value = ((SqlBigIntegerLiteralValue) rightLiteral).getBigInteger().longValue();
                final String tableName = getTableName(leftColumn);
                final String columnName = leftColumn.getColumnName();
                switch (e.getSqlComparisonOperator()) {
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
                        throw new RuntimeException("Unexpected comparison operator " + e.getSqlComparisonOperator());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported literal " + e.getSqlComparisonOperator());
            }

        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }

    }


    @Override
    public List<SchemaConstraint> visit(SqlInCondition inExpression, Void argument) {

        SqlColumnName column = inExpression.getSqlColumnName();
        String columnName = column.getColumnName();
        SqlConditionList rightItemsList = inExpression.getLiteralList();

        List<String> stringValues = new LinkedList<>();
        for (SqlCondition expressionValue : rightItemsList.getSqlConditionExpressions()) {
            final String stringValue;
            if (expressionValue instanceof SqlStringLiteralValue) {
                stringValue = new StringValue(expressionValue.toSql()).getNotExcapedValue();
            } else {
                stringValue = expressionValue.toSql();
            }
            stringValues.add(stringValue);
        }
        return Collections.singletonList(new EnumConstraint(columnName, stringValues));

    }

    @Override
    public List<SchemaConstraint> visit(SqlNullLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlStringLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlConditionList e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlIsNotNullCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlBinaryDataLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlSimilarToCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlIsNullCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlLikeCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlOrCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlBigDecimalLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlBigIntegerLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlBooleanLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public List<SchemaConstraint> visit(SqlColumnName e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }


}
