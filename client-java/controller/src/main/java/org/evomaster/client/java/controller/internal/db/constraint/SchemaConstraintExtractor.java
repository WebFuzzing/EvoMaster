package org.evomaster.client.java.controller.internal.db.constraint;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SchemaConstraintExtractor implements ExpressionVisitor {

    private final List<SchemaConstraint> constraints = new ArrayList<>();

    /**
     * Return the constraints collected during the visit to the AST
     *
     * @return
     */
    public List<SchemaConstraint> getConstraints() {
        return this.constraints;
    }


    /**
     * FIXME
     * temporary workaround before major refactoring.
     * Recall that Column.getTable() is not reliable
     */
    private String getTableName(Column column) {
        Table table = column.getTable();
        if (table != null) {
            return table.getName();
        }

        return "?";
    }

    @Override
    public void visit(BitwiseRightShift aThis) {
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseLeftShift aThis) {
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NextValExpression aThis) {
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(CollateExpression aThis) {
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ValueListExpression valueList) {

    }

//    @Override
//    public void visit(WithinGroupExpression wgexpr) {
//
//    }

    @Override
    public void visit(NullValue nullValue) {
        // TODO This translation should be implemented

        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Function function) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(SignedExpression signedExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DoubleValue doubleValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(LongValue longValue) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(HexValue hexValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DateValue dateValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimeValue timeValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimestampValue timestampValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Addition addition) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Division division) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Multiplication multiplication) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Subtraction subtraction) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.getLeftExpression().accept(this);
        andExpression.getRightExpression().accept(this);
    }

    @Override
    public void visit(OrExpression orExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Between between) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        Expression left = equalsTo.getLeftExpression();
        Expression right = equalsTo.getRightExpression();

        if (left instanceof LongValue && right instanceof Column) {
            // matches value = column
            LongValue leftLongValue = (LongValue) left;
            Column rightColumn = (Column) right;
            long value = leftLongValue.getValue();
            String tableName = getTableName(rightColumn);
            String columnName = rightColumn.getColumnName();
            RangeConstraint rangeConstraint = new RangeConstraint(tableName, columnName, value, value);
            constraints.add(rangeConstraint);
        } else if (left instanceof Column && right instanceof LongValue) {
            // matches column = value
            Column leftColumn = (Column) left;
            LongValue rightLongValue = (LongValue) right;
            long value = rightLongValue.getValue();
            String tableName = getTableName(leftColumn);
            String columnName = leftColumn.getColumnName();
            RangeConstraint rangeConstraint = new RangeConstraint(tableName, columnName, value, value);
            constraints.add(rangeConstraint);
        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }
    }


    @Override
    public void visit(GreaterThan greaterThan) {
        Expression left = greaterThan.getLeftExpression();
        Expression right = greaterThan.getRightExpression();

        if (left instanceof LongValue && right instanceof Column) {
            // matches value > column
            LongValue leftLongValue = (LongValue) left;
            Column rightColumn = (Column) right;
            long upperBound = leftLongValue.getValue();
            String tableName = getTableName(rightColumn);
            String columnName = rightColumn.getColumnName();
            UpperBoundConstraint upperBoundConstraint = new UpperBoundConstraint(tableName, columnName, upperBound - 1);
            constraints.add(upperBoundConstraint);
        } else if (left instanceof Column && right instanceof LongValue) {
            // matches column > value
            Column leftColumn = (Column) left;
            LongValue rightLongValue = (LongValue) right;
            long lowerBound = rightLongValue.getValue();
            String tableName = getTableName(leftColumn);
            String columnName = leftColumn.getColumnName();
            LowerBoundConstraint lowerBoundConstraint = new LowerBoundConstraint(tableName, columnName, lowerBound + 1);
            constraints.add(lowerBoundConstraint);
        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        Expression left = greaterThanEquals.getLeftExpression();
        Expression right = greaterThanEquals.getRightExpression();

        if (left instanceof LongValue && right instanceof Column) {
            // matches value >= column
            LongValue leftLongValue = (LongValue) left;
            Column rightColumn = (Column) right;
            long upperBound = leftLongValue.getValue();
            String tableName = getTableName(rightColumn);
            String columnName = rightColumn.getColumnName();
            UpperBoundConstraint upperBoundConstraint = new UpperBoundConstraint(tableName, columnName, upperBound);
            constraints.add(upperBoundConstraint);
        } else if (left instanceof Column && right instanceof LongValue) {
            // matches column >= value
            Column leftColumn = (Column) left;
            LongValue rightLongValue = (LongValue) right;
            long lowerBound = rightLongValue.getValue();
            String tableName = getTableName(leftColumn);
            String columnName = leftColumn.getColumnName();
            LowerBoundConstraint lowerBoundConstraint = new LowerBoundConstraint(tableName, columnName, lowerBound);
            constraints.add(lowerBoundConstraint);
        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }

    }

    @Override
    public void visit(InExpression inExpression) {
        Expression leftExpression = inExpression.getLeftExpression();
        if (!(leftExpression instanceof Column)) {
            throw new RuntimeException("Must implement InExpression with left " + leftExpression.getClass().getName());
        }
        Column column = (Column) leftExpression;
        String columnName = column.getColumnName();
        ItemsList rightItemsList = inExpression.getRightItemsList();
        if (!(rightItemsList instanceof ItemsList)) {
            throw new RuntimeException("Must implement InExpression with right " + rightItemsList.getClass().getName());
        }
        if (rightItemsList instanceof ExpressionList) {
            ExpressionList expressionList = (ExpressionList) rightItemsList;
            List<String> stringValues = new LinkedList<>();
            for (Expression expressionValue : expressionList.getExpressions()) {
                String stringValue;
                if (expressionValue instanceof StringValue) {
                    StringValue expressionStringValue = (StringValue) expressionValue;
                    stringValue = expressionStringValue.getNotExcapedValue();
                } else {
                    stringValue = expressionValue.toString();
                }
                stringValues.add(stringValue);
            }
            EnumConstraint enumConstraint = new EnumConstraint(columnName, stringValues);
            this.constraints.add(enumConstraint);
        }
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(LikeExpression likeExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(MinorThan minorThan) {
        Expression left = minorThan.getLeftExpression();
        Expression right = minorThan.getRightExpression();

        if (left instanceof LongValue && right instanceof Column) {
            // matches value < column
            LongValue leftLongValue = (LongValue) left;
            Column rightColumn = (Column) right;
            long lowerBound = leftLongValue.getValue();
            String tableName = getTableName(rightColumn);
            String columnName = rightColumn.getColumnName();
            LowerBoundConstraint lowerBoundConstraint = new LowerBoundConstraint(tableName, columnName, lowerBound + 1);
            constraints.add(lowerBoundConstraint);
        } else if (left instanceof Column && right instanceof LongValue) {
            // matches column < value
            Column leftColumn = (Column) left;
            LongValue rightLongValue = (LongValue) right;
            long upperBound = rightLongValue.getValue();
            String tableName = getTableName(leftColumn);
            String columnName = leftColumn.getColumnName();
            UpperBoundConstraint upperBoundConstraint = new UpperBoundConstraint(tableName, columnName, upperBound - 1);
            constraints.add(upperBoundConstraint);
        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        Expression left = minorThanEquals.getLeftExpression();
        Expression right = minorThanEquals.getRightExpression();

        if (left instanceof LongValue && right instanceof Column) {
            // matches value <= column
            LongValue leftLongValue = (LongValue) left;
            Column rightColumn = (Column) right;
            long lowerBound = leftLongValue.getValue();
            String tableName = getTableName(rightColumn);
            String columnName = rightColumn.getColumnName();
            LowerBoundConstraint lowerBoundConstraint = new LowerBoundConstraint(tableName, columnName, lowerBound);
            constraints.add(lowerBoundConstraint);
        } else if (left instanceof Column && right instanceof LongValue) {
            // matches column <= value
            Column leftColumn = (Column) left;
            LongValue rightLongValue = (LongValue) right;
            long upperBound = rightLongValue.getValue();
            String tableName = getTableName(leftColumn);
            String columnName = leftColumn.getColumnName();
            UpperBoundConstraint upperBoundConstraint = new UpperBoundConstraint(tableName, columnName, upperBound);
            constraints.add(upperBoundConstraint);
        } else {
            // TODO This translation should be implemented
            throw new RuntimeException("Extraction of condition not yet implemented");
        }

    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Column column) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(SubSelect subSelect) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(CaseExpression caseExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(WhenClause whenClause) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ExistsExpression existsExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AllComparisonExpression allComparisonExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Concat concat) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Matches matches) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(CastExpression castExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Modulo modulo) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }


    @Override
    public void visit(ExtractExpression extractExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JsonExpression jsonExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(RegExpMySQLOperator regExpMySQLOperator) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(UserVariable userVariable) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NumericBind numericBind) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(KeepExpression keepExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }


    @Override
    public void visit(RowConstructor rowConstructor) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(OracleHint oracleHint) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NotExpression notExpression) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }


}
