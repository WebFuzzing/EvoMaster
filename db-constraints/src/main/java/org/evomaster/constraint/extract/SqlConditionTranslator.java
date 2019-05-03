package org.evomaster.constraint.extract;


import net.sf.jsqlparser.expression.StringValue;
import org.evomaster.constraint.*;
import org.evomaster.constraint.ast.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlConditionTranslator extends SqlConditionVisitor<TableConstraint, Void> {

    private final TranslationContext translationContext;

    public SqlConditionTranslator(TranslationContext translationContext) {
        this.translationContext = translationContext;
    }

    /**
     * FIXME
     * temporary workaround before major refactoring.
     * Recall that Column.getTable() is not reliable
     */
    private String getTableName(SqlColumn column) {
        String tableName = column.getTableName();
        if (tableName != null) {
            return tableName;
        } else {
            return this.translationContext.getCurrentTableName();
        }
    }


    @Override
    public TableConstraint visit(SqlAndCondition andExpression, Void argument) {
        TableConstraint left = andExpression.getLeftExpr().accept(this, null);
        TableConstraint right = andExpression.getRightExpr().accept(this, null);
        return new AndConstraint(translationContext.getCurrentTableName(), left, right);
    }

    @Override
    public TableConstraint visit(SqlComparisonCondition e, Void argument) {
        SqlCondition left = e.getLeftOperand();
        SqlCondition right = e.getRightOperand();

        if (left instanceof SqlLiteralValue && right instanceof SqlColumn) {
            SqlLiteralValue leftLiteral = (SqlLiteralValue) left;
            SqlColumn rightColumn = (SqlColumn) right;
            if (leftLiteral instanceof SqlBigIntegerLiteralValue) {
                long value = ((SqlBigIntegerLiteralValue) leftLiteral).getBigInteger().longValue();
                final String tableName = getTableName(rightColumn);
                final String columnName = rightColumn.getColumnName();
                switch (e.getSqlComparisonOperator()) {
                    case EQUALS_TO: {
                        return new RangeConstraint(tableName, columnName, value, value);
                    }
                    case GREATER_THAN: {
                        return new UpperBoundConstraint(tableName, columnName, value - 1);
                    }
                    case GREATER_THAN_OR_EQUAL: {
                        return new UpperBoundConstraint(tableName, columnName, value);
                    }
                    case LESS_THAN: {
                        return new LowerBoundConstraint(tableName, columnName, value + 1);
                    }
                    case LESS_THAN_OR_EQUAL: {
                        return new LowerBoundConstraint(tableName, columnName, value);
                    }
                    default: {
                        throw new RuntimeException("Unexpected comparison operator " + e.getSqlComparisonOperator());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported literal " + e.getSqlComparisonOperator());
            }
        } else if (left instanceof SqlColumn && right instanceof SqlLiteralValue) {
            SqlColumn leftColumn = (SqlColumn) left;
            SqlLiteralValue rightLiteral = (SqlLiteralValue) right;
            if (rightLiteral instanceof SqlBigIntegerLiteralValue) {
                long value = ((SqlBigIntegerLiteralValue) rightLiteral).getBigInteger().longValue();
                final String tableName = getTableName(leftColumn);
                final String columnName = leftColumn.getColumnName();
                switch (e.getSqlComparisonOperator()) {
                    case EQUALS_TO: {
                        return new RangeConstraint(tableName, columnName, value, value);
                    }
                    case GREATER_THAN: {
                        return new LowerBoundConstraint(tableName, columnName, value + 1);
                    }
                    case GREATER_THAN_OR_EQUAL: {
                        return new LowerBoundConstraint(tableName, columnName, value);
                    }
                    case LESS_THAN: {
                        return new UpperBoundConstraint(tableName, columnName, value - 1);
                    }
                    case LESS_THAN_OR_EQUAL: {
                        return new UpperBoundConstraint(tableName, columnName, value);
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
    public TableConstraint visit(SqlInCondition inExpression, Void argument) {
        SqlColumn column = inExpression.getSqlColumn();
        String tableName = getTableName(column);
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
        return new EnumConstraint(tableName, columnName, stringValues);

    }

    @Override
    public TableConstraint visit(SqlNullLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlStringLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlConditionList e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlIsNotNullCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlBinaryDataLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlSimilarToCondition e, Void argument) {
        String tableName = getTableName(e.getColumn());
        return new SimilarToConstraint(tableName, e.getColumn().getColumnName(), e.getPattern().toSql());
    }

    @Override
    public TableConstraint visit(SqlIsNullCondition e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlLikeCondition e, Void argument) {
        String tableName = getTableName(e.getColumnName());
        String columnName = e.getColumnName().getColumnName();
        String pattern = e.getPattern().toSql();
        return new LikeConstraint(tableName, columnName, pattern);
    }

    @Override
    public TableConstraint visit(SqlOrCondition e, Void argument) {
        List<TableConstraint> orConstraints = e.getOrConditions().stream().map(c -> c.accept(this, null)).collect(Collectors.toList());
        OrConstraint orConstraint = new OrConstraint(translationContext.getCurrentTableName(), orConstraints.toArray(new TableConstraint[]{}));
        return orConstraint;
    }

    @Override
    public TableConstraint visit(SqlBigDecimalLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlBigIntegerLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlBooleanLiteralValue e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }

    @Override
    public TableConstraint visit(SqlColumn e, Void argument) {
        throw new UnsupportedOperationException("This method should not be invoked");
    }


}
