package org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;
import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;
import org.evomaster.client.java.sql.distance.advanced.evaluation_context.EvaluationContext;
import org.evomaster.client.java.sql.distance.advanced.helpers.distance.BranchDistanceHelper;
import org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance.expressions.SelectComparisonExpression;
import org.evomaster.client.java.sql.distance.advanced.select_query.SelectQuery;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.createDistance;
import static org.evomaster.client.java.sql.distance.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToDouble;
import static org.evomaster.client.java.sql.distance.advanced.helpers.LiteralsHelper.isBooleanLiteral;
import static org.evomaster.client.java.sql.distance.advanced.helpers.LiteralsHelper.isTrueLiteral;

public class WhereDistanceCalculator implements ExpressionVisitor {

    public static final char MINUS_SIGN = '-';

    private Expression where;
    private EvaluationContext evaluationContext;
    private WhereDistanceStack stack;

    private WhereDistanceCalculator(Expression where, EvaluationContext evaluationContext) {
        this.where = where;
        this.evaluationContext = evaluationContext;
        this.stack = new WhereDistanceStack();
    }

    public static WhereDistanceCalculator createWhereDistanceCalculator(SelectQuery query, Row row) {
        EvaluationContext evaluationContext = createEvaluationContext(row);
        return new WhereDistanceCalculator(query.getWhere(), evaluationContext);
    }

    public Distance calculate() {
        where.accept(this);
        Distance distance = stack.popDistance();
        SimpleLogger.debug(format("Distance between between WHERE: %s and row: %s was %s", where, evaluationContext, distance));
        return distance;
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(NullValue nullValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Function function) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        signedExpression.getExpression().accept(this);
        Double numberWithoutSign = convertToDouble(stack.popNumber());
        Double number = signedExpression.getSign() != MINUS_SIGN ? numberWithoutSign : -numberWithoutSign;
        stack.pushNumber(number);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        stack.pushNumber(doubleValue.getValue());
    }

    @Override
    public void visit(LongValue longValue) {
        stack.pushNumber(longValue.getValue());
    }

    @Override
    public void visit(HexValue hexValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(DateValue dateValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TimeValue timeValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(StringValue stringValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Addition addition) {
        addition.getRightExpression().accept(this);
        addition.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) + convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(Division division) {
        division.getRightExpression().accept(this);
        division.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) / convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(IntegerDivision integerDivision) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Multiplication multiplication) {
        multiplication.getRightExpression().accept(this);
        multiplication.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) * convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(Subtraction subtraction) {
        subtraction.getRightExpression().accept(this);
        subtraction.getLeftExpression().accept(this);
        Double number = convertToDouble(stack.popNumber()) - convertToDouble(stack.popNumber());
        stack.pushNumber(number);
    }

    @Override
    public void visit(AndExpression andExpression) {
        andExpression.getRightExpression().accept(this);
        andExpression.getLeftExpression().accept(this);
        stack.pushDistance(aggregateDistancesForAnd(stack.popDistance(), stack.popDistance()));
    }

    private Distance aggregateDistancesForAnd(Distance aDistance, Distance otherDistance) {
        return createDistance(
            BranchDistanceHelper.aggregateDistancesForAnd(aDistance.getBranchDistance(), otherDistance.getBranchDistance()));
    }

    @Override
    public void visit(OrExpression orExpression) {
        orExpression.getRightExpression().accept(this);
        orExpression.getLeftExpression().accept(this);
        stack.pushDistance(aggregateDistancesForOr(stack.popDistance(), stack.popDistance()));
    }

    private Distance aggregateDistancesForOr(Distance aDistance, Distance otherDistance) {
        return createDistance(
            BranchDistanceHelper.aggregateDistancesForOr(aDistance.getBranchDistance(), otherDistance.getBranchDistance()));
    }

    @Override
    public void visit(XorExpression xorExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Between between) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        calculateDistanceForComparisonOperator(equalsTo);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        calculateDistanceForComparisonOperator(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        calculateDistanceForComparisonOperator(greaterThanEquals);
    }

    private void calculateDistanceForComparisonOperator(ComparisonOperator operator) {
        operator.getRightExpression().accept(this);
        operator.getLeftExpression().accept(this);

        Object leftValue = stack.popGenericExpression();
        Object rightValue = stack.popGenericExpression();

        if(rightValue instanceof SelectComparisonExpression) {
            throw new UnsupportedOperationException("This SQL feature is not supported yet");
        } else {
            stack.pushDistance(createDistance(getBranchDistanceFunction(operator).apply(leftValue, rightValue)));
        }
    }

    private BiFunction<Object, Object, Double> getBranchDistanceFunction(ComparisonOperator comparisonOperator) {
        if(comparisonOperator instanceof EqualsTo) {
            return BranchDistanceHelper::calculateDistanceForEquals;
        } else if(comparisonOperator instanceof NotEqualsTo) {
            return BranchDistanceHelper::calculateDistanceForNotEquals;
        } else if(comparisonOperator instanceof GreaterThan) {
            return BranchDistanceHelper::calculateDistanceForGreaterThan;
        } else if(comparisonOperator instanceof GreaterThanEquals) {
            return BranchDistanceHelper::calculateDistanceForGreaterThanOrEquals;
        } else if(comparisonOperator instanceof MinorThan) {
            return BranchDistanceHelper::calculateDistanceForMinorThan;
        } else if(comparisonOperator instanceof MinorThanEquals) {
            return BranchDistanceHelper::calculateDistanceForMinorThanOrEquals;
        } else {
            throw new UnsupportedOperationException("This comparison operator is not supported yet");
        }
    }

    @Override
    public void visit(InExpression inExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(MinorThan minorThan) {
        calculateDistanceForComparisonOperator(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        calculateDistanceForComparisonOperator(minorThanEquals);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        calculateDistanceForComparisonOperator(notEqualsTo);
    }

    @Override
    public void visit(ParenthesedSelect parenthesedSelect) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Column column) {
        if(evaluationContext.includes(column)) {
            Object value = evaluationContext.getValue(column);
            if(value instanceof Number) {
                stack.pushNumber((Number) value);
            } else {
                stack.pushGenericExpression(value);
            }
        } else if(isBooleanLiteral(column.getColumnName())) {
            stack.pushBoolean(isTrueLiteral(column.getColumnName()));
        } else {
            throw new AssertionError(format("Column %s must be present in %s or be a boolean literal", column, evaluationContext));
        }
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(WhenClause whenClause) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        stack.pushGenericExpression(
            new SelectComparisonExpression(
                anyComparisonExpression.getSelect(), anyComparisonExpression.getAnyType()));
    }

    @Override
    public void visit(Concat concat) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Matches matches) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(CastExpression castExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Modulo modulo) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ExtractExpression extractExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(JsonExpression jsonExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(UserVariable userVariable) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(NumericBind numericBind) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(KeepExpression keepExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ExpressionList<?> expressionList) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(RowConstructor<?> rowConstructor) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(OracleHint oracleHint) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(NotExpression notExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(SimilarToExpression similarToExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(XMLSerializeExpr xmlSerializeExpr) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TimezoneExpression timezoneExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(JsonAggregateFunction jsonAggregateFunction) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(JsonFunction jsonFunction) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(AllColumns allColumns) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(AllValue allValue) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(Select select) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        throw new UnsupportedOperationException("This SQL feature is not supported yet");
    }
}