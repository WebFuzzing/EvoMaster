package org.evomaster.dbconstraint.parser.jsql;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.CollateExpression;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.ValueListExpression;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseLeftShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NamedExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.evomaster.dbconstraint.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JSqlVisitor implements ExpressionVisitor, ItemsListVisitor {

    private final Deque<SqlCondition> stack = new ArrayDeque<>();

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(NullValue nullValue) {
        stack.push(new SqlNullLiteralValue());
    }

    @Override
    public void visit(Function function) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
        SqlCondition sqlCondition = stack.pop();
        if (sqlCondition instanceof SqlLiteralValue) {
            SqlLiteralValue sqlLiteralValue = (SqlLiteralValue) sqlCondition;
            SqlLiteralValue negated;
            if (sqlLiteralValue instanceof SqlBigIntegerLiteralValue) {
                SqlBigIntegerLiteralValue sqlBigIntegerLiteralValue = (SqlBigIntegerLiteralValue) sqlLiteralValue;
                negated = new SqlBigIntegerLiteralValue(sqlBigIntegerLiteralValue.getBigInteger().negate());
            } else if (sqlLiteralValue instanceof SqlBigDecimalLiteralValue) {
                SqlBigDecimalLiteralValue sqlBigDecimalLiteralValue = (SqlBigDecimalLiteralValue) sqlLiteralValue;
                negated = new SqlBigDecimalLiteralValue(sqlBigDecimalLiteralValue.getBigDecimal().negate());

            } else {
                throw new RuntimeException("Extraction of condition not yet implemented for literal value class " + sqlLiteralValue.getClass());
            }
            stack.push(negated);
        } else {
            throw new RuntimeException("Extraction of condition not yet implemented for " + sqlCondition.getClass());
        }
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
        stack.push(new SqlBigDecimalLiteralValue(doubleValue.getValue()));
    }

    @Override
    public void visit(LongValue longValue) {
        stack.push(new SqlBigIntegerLiteralValue(longValue.getBigIntegerValue()));
    }

    @Override
    public void visit(HexValue hexValue) {
        stack.push(new SqlBinaryDataLiteralValue(hexValue.getValue()));
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
        String notEscapedValue = stringValue.getNotExcapedValue();

        String notEscapedValueNoQuotes;
        if (notEscapedValue.startsWith("'") && notEscapedValue.endsWith("'")) {
            notEscapedValueNoQuotes = notEscapedValue.substring(1, notEscapedValue.length() - 1);
        } else {
            notEscapedValueNoQuotes = notEscapedValue;
        }
        stack.push(new SqlStringLiteralValue(notEscapedValueNoQuotes));
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
        SqlCondition left = stack.pop();
        andExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlAndCondition(left, right));
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        orExpression.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlOrCondition(left, right));
    }

    @Override
    public void visit(Between between) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        equalsTo.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        equalsTo.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.EQUALS_TO, right));
    }


    @Override
    public void visit(GreaterThan greaterThan) {
        greaterThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN, right));
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        greaterThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        greaterThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.GREATER_THAN_OR_EQUAL, right));
    }

    @Override
    public void visit(InExpression inExpression) {
        inExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        inExpression.getRightItemsList().accept(this);
        SqlConditionList right = (SqlConditionList) stack.pop();
        stack.push(new SqlInCondition(left, right));
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        isNullExpression.getLeftExpression().accept(this);
        SqlColumn columnName = (SqlColumn) stack.pop();
        if (isNullExpression.isNot()) {
            stack.push(new SqlIsNotNullCondition(columnName));
        } else {
            stack.push(new SqlIsNullCondition(columnName));
        }
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        likeExpression.getLeftExpression().accept(this);
        SqlColumn left = (SqlColumn) stack.pop();
        likeExpression.getRightExpression().accept(this);
        SqlStringLiteralValue pattern = (SqlStringLiteralValue) stack.pop();
        stack.push(new SqlLikeCondition(left, pattern));
    }

    @Override
    public void visit(MinorThan minorThan) {
        minorThan.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThan.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN, right));
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        minorThanEquals.getLeftExpression().accept(this);
        SqlCondition left = stack.pop();
        minorThanEquals.getRightExpression().accept(this);
        SqlCondition right = stack.pop();
        stack.push(new SqlComparisonCondition(left, SqlComparisonOperator.LESS_THAN_OR_EQUAL, right));
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(Column column) {
        String columnName = column.getColumnName();
        if (column.getTable() != null) {
            String tableName = column.getTable().getName();
            stack.push(new SqlColumn(tableName, columnName));
        } else {
            stack.push(new SqlColumn(columnName));
        }
    }

    @Override
    public void visit(SubSelect subSelect) {

        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(ExpressionList expressionList) {
        List<SqlCondition> sqlConditionList = new ArrayList<>();
        for (Expression expr : expressionList.getExpressions()) {
            expr.accept(this);
            SqlCondition sqlCondition = stack.pop();
            sqlConditionList.add(sqlCondition);
        }
        stack.push(new SqlConditionList(sqlConditionList));
    }

    @Override
    public void visit(NamedExpressionList namedExpressionList) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(MultiExpressionList multiExpressionList) {
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

    /**
     * e.g. 'hi'::text
     *
     * @param castExpression
     */
    @Override
    public void visit(CastExpression castExpression) {
        castExpression.getLeftExpression().accept(this);
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

        regExpMatchOperator.getLeftExpression().accept(this);
        SqlColumn columnName = (SqlColumn) this.stack.pop();

        String operator1 = regExpMatchOperator.getStringExpression();
        if (!operator1.equals("~")) {
            throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
        }

        if (regExpMatchOperator.getRightExpression() instanceof SignedExpression) {
            SignedExpression signedRightExpression = (SignedExpression) regExpMatchOperator.getRightExpression();
            String operator2 = String.valueOf(signedRightExpression.getSign());
            if (!operator2.equals("~")) {
                throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
            }
            signedRightExpression.getExpression().accept(this);
            SqlStringLiteralValue pattern = (SqlStringLiteralValue) this.stack.pop();

            stack.push(new SqlLikeCondition(columnName, pattern));

        } else if (regExpMatchOperator.getRightExpression() instanceof Function) {
            Function function = (Function) regExpMatchOperator.getRightExpression();
            if (function.equals("similar_escape")) {
                throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
            }
            function.getParameters().accept(this);
            SqlConditionList parameterList = (SqlConditionList) stack.pop();
            SqlStringLiteralValue pattern = (SqlStringLiteralValue) parameterList.getSqlConditionExpressions().get(0);
            stack.push(new SqlSimilarToCondition(columnName, pattern));

        } else {
            throw new IllegalArgumentException("Unsupported regular expression match " + regExpMatchOperator);
        }

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
    public void visit(ValueListExpression valueListExpression) {

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

    @Override
    public void visit(NextValExpression nextValExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        // TODO This translation should be implemented
        throw new RuntimeException("Extraction of condition not yet implemented");
    }

    /**
     * Return the constraints collected during the visit to the AST
     *
     * @return
     */
    public SqlCondition getSqlCondition() {
        return this.stack.peek();
    }
}
