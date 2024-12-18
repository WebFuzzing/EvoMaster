package org.evomaster.client.java.sql.internal;

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
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Stack;

public class SqlExpressionEvaluator extends ExpressionVisitorAdapter {

    private final DataRow dataRow;
    private final SqlNameContext sqlNameContext;
    private final Stack<Truthness> computedTruthnesses = new Stack<>();
    private final Stack<Object> concreteValues = new Stack<>();

    public SqlExpressionEvaluator(SqlNameContext sqlNameContext, DataRow dataRow) {
        this.sqlNameContext = sqlNameContext;
        this.dataRow = dataRow;
    }


    public Truthness getEvaluatedTruthness() {
        if (computedTruthnesses.isEmpty()) {
            throw new IllegalStateException("no Truthness was computed");
        }
        return computedTruthnesses.peek();
    }

    private void visitComparisonOperator(ComparisonOperator comparisonOperator) {
        final Object concreteRightValue = concreteValues.pop();
        final Object concreteLeftValue = concreteValues.pop();
        final Truthness truthness = computeTruthnessForComparisonOperator(concreteLeftValue, concreteRightValue, comparisonOperator);
        computedTruthnesses.push(truthness);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        super.visit(equalsTo);
        visitComparisonOperator(equalsTo);
    }

    private Truthness computeTruthnessForComparisonOperator(Object concreteLeftValue, Object concreteRightValue, ComparisonOperator comparisonOperator) {
        final Truthness truthness;
        if (concreteLeftValue == null && concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS;
        } else if (concreteLeftValue == null || concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS_BETTER;
        } else {
            final Truthness truthnessOfExpression;
            if (concreteLeftValue instanceof Number && concreteRightValue instanceof Number) {
                double leftValueAsDouble = ((Number) concreteLeftValue).doubleValue();
                double rightValueAsDouble = ((Number) concreteRightValue).doubleValue();
                if (comparisonOperator instanceof EqualsTo) {
                    truthnessOfExpression = TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble);
                } else if (comparisonOperator instanceof NotEqualsTo) {
                    //a != b => !(a == b)
                    truthnessOfExpression = TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble).invert();
                } else if (comparisonOperator instanceof GreaterThan) {
                    //a > b => b < a
                    truthnessOfExpression = TruthnessUtils.getLessThanTruthness(rightValueAsDouble, leftValueAsDouble);
                } else if (comparisonOperator instanceof MinorThan) {
                    truthnessOfExpression = TruthnessUtils.getLessThanTruthness(leftValueAsDouble, rightValueAsDouble);
                } else if (comparisonOperator instanceof MinorThanEquals) {
                    //a <= b => b >= a => !(b < a)
                    truthnessOfExpression = TruthnessUtils.getLessThanTruthness(rightValueAsDouble, leftValueAsDouble).invert();
                } else if (comparisonOperator instanceof GreaterThanEquals) {
                    //a >= b => ! (a < b)
                    truthnessOfExpression = TruthnessUtils.getLessThanTruthness(leftValueAsDouble, rightValueAsDouble).invert();
                } else {
                    throw new UnsupportedOperationException("Unsupported comparison operator: " + comparisonOperator);
                }
            } else if (concreteRightValue instanceof String && concreteLeftValue instanceof String) {
                throw new UnsupportedOperationException("String comparison not yet supported");
            } else if (concreteLeftValue instanceof Timestamp || concreteRightValue instanceof Timestamp
                    || concreteLeftValue instanceof Instant || concreteRightValue instanceof Instant) {
                throw new UnsupportedOperationException("Timestamp/Instant comparison not yet supported");
            } else if (concreteLeftValue instanceof Boolean && concreteRightValue instanceof Boolean) {
                throw new UnsupportedOperationException("Boolean comparison not yet supported");
            } else {
                throw new UnsupportedOperationException("type not supported");
            }
            if (truthnessOfExpression.isTrue()) {
                truthness = truthnessOfExpression;
            } else {
                truthness = TruthnessUtils.buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, truthnessOfExpression.getOfTrue());
            }
        }
        return truthness;
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        throw new UnsupportedOperationException("Bitwise right shift not supported");
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        throw new UnsupportedOperationException("Bitwise left shift not supported");
    }

    @Override
    public void visit(NullValue nullValue) {
        throw new UnsupportedOperationException("Null value not supported");
    }

    @Override
    public void visit(Function function) {
        throw new UnsupportedOperationException("Function not supported");
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        throw new UnsupportedOperationException("Signed expression not supported");
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        throw new UnsupportedOperationException("JdbcParameter not supported");
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        throw new UnsupportedOperationException("JdbcNamedParameter not supported");
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        throw new UnsupportedOperationException("DoubleValue not supported");
    }

    @Override
    public void visit(LongValue longValue) {
        long concreteValue = longValue.getValue();
        concreteValues.push(concreteValue);
    }

    @Override
    public void visit(HexValue hexValue) {
        throw new UnsupportedOperationException("HexValue not supported");
    }

    @Override
    public void visit(DateValue dateValue) {
        throw new UnsupportedOperationException("DateValue not supported");
    }

    @Override
    public void visit(TimeValue timeValue) {
        throw new UnsupportedOperationException("TimeValue not supported");
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        throw new UnsupportedOperationException("TimestampValue not supported");
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        throw new UnsupportedOperationException("Parenthesis not supported");
    }


    @Override
    public void visit(IntegerDivision integerDivision) {
        throw new UnsupportedOperationException("visit(IntegerDivision) not supported");
    }

    @Override
    public void visit(Multiplication multiplication) {
        throw new UnsupportedOperationException("visit(Multiplication) not supported");
    }

    @Override
    public void visit(Subtraction subtraction) {
        throw new UnsupportedOperationException("visit(Subtraction) not supported");
    }

    @Override
    public void visit(AndExpression andExpression) {
        throw new UnsupportedOperationException("visit(AndExpression) not supported");
    }

    @Override
    public void visit(OrExpression orExpression) {
        throw new UnsupportedOperationException("visit(OrExpression) not supported");
    }

    @Override
    public void visit(XorExpression xorExpression) {
        throw new UnsupportedOperationException("visit(XorExpression) not supported");
    }

    @Override
    public void visit(Between between) {
        throw new UnsupportedOperationException("visit(Between) not supported");
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        throw new UnsupportedOperationException("visit(OverlapsCondition) not supported");
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        super.visit(greaterThan);
        this.visitComparisonOperator(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        super.visit(greaterThanEquals);
        this.visitComparisonOperator(greaterThanEquals);
    }

    @Override
    public void visit(InExpression inExpression) {
        throw new UnsupportedOperationException("visit(InExpression) not supported");
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        throw new UnsupportedOperationException("visit(FullTextSearch) not supported");
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        throw new UnsupportedOperationException("visit(IsNullExpression) not supported");
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        throw new UnsupportedOperationException("visit(IsBooleanExpression) not supported");
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        throw new UnsupportedOperationException("visit(LikeExpression) not supported");
    }

    @Override
    public void visit(MinorThan minorThan) {
        super.visit(minorThan);
        visitComparisonOperator(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        super.visit(minorThanEquals);
        visitComparisonOperator(minorThanEquals);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        super.visit(notEqualsTo);
        visitComparisonOperator(notEqualsTo);
    }

    @Override
    public void visit(DoubleAnd doubleAnd) {
        throw new UnsupportedOperationException("visit(DoubleAnd) not supported");
    }

    @Override
    public void visit(Contains contains) {
        throw new UnsupportedOperationException("visit(Contains) not supported");
    }

    @Override
    public void visit(ContainedBy containedBy) {
        throw new UnsupportedOperationException("visit(ContainedBy) not supported");
    }

    @Override
    public void visit(ParenthesedSelect parenthesedSelect) {
        throw new UnsupportedOperationException("visit(ParenthesedSelect) not supported");
    }

    @Override
    public void visit(Column column) {
        String name = column.getColumnName();
        String table = sqlNameContext.getTableName(column);
        Object value = dataRow.getValueByName(name, table);
        concreteValues.push(value);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        throw new UnsupportedOperationException("visit(CaseExpression) not supported");
    }

    @Override
    public void visit(WhenClause whenClause) {
        throw new UnsupportedOperationException("visit(WhenClause) not supported");
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        throw new UnsupportedOperationException("visit(ExistsExpression) not supported");
    }

    @Override
    public void visit(MemberOfExpression memberOfExpression) {
        throw new UnsupportedOperationException("visit(MemberOfExpression) not supported");
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        throw new UnsupportedOperationException("visit(AnyComparisonExpression) not supported");
    }

    @Override
    public void visit(Concat concat) {
        throw new UnsupportedOperationException("visit(Concat) not supported");
    }

    @Override
    public void visit(Matches matches) {
        throw new UnsupportedOperationException("visit(Matches) not supported");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        throw new UnsupportedOperationException("visit(BitwiseAnd) not supported");
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        throw new UnsupportedOperationException("visit(BitwiseOr) not supported");
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        throw new UnsupportedOperationException("visit(BitwiseXor) not supported");
    }

    @Override
    public void visit(CastExpression castExpression) {
        throw new UnsupportedOperationException("visit(CastExpression) not supported");
    }

    @Override
    public void visit(Modulo modulo) {
        throw new UnsupportedOperationException("visit(Modulo) not supported");
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {
        throw new UnsupportedOperationException("visit(AnalyticExpression) not supported");
    }

    @Override
    public void visit(ExtractExpression extractExpression) {
        throw new UnsupportedOperationException("visit(ExtractExpression) not supported");
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {
        throw new UnsupportedOperationException("visit(IntervalExpression) not supported");
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        throw new UnsupportedOperationException("visit(OracleHierarchicalExpression) not supported");
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {
        throw new UnsupportedOperationException("visit(RegExpMatchOperator) not supported");
    }

    @Override
    public void visit(JsonExpression jsonExpression) {
        throw new UnsupportedOperationException("visit(JsonExpression) not supported");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        throw new UnsupportedOperationException("visit(JsonOperator) not supported");
    }

    @Override
    public void visit(UserVariable userVariable) {
        throw new UnsupportedOperationException("visit(UserVariable) not supported");
    }

    @Override
    public void visit(NumericBind numericBind) {
        throw new UnsupportedOperationException("visit(NumericBind) not supported");
    }

    @Override
    public void visit(KeepExpression keepExpression) {
        throw new UnsupportedOperationException("visit(KeepExpression) not supported");
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        throw new UnsupportedOperationException("visit(MySQLGroupConcat) not supported");
    }

    @Override
    public void visit(ExpressionList<?> expressionList) {
        throw new UnsupportedOperationException("visit(ExpressionList) not supported");
    }

    @Override
    public void visit(RowConstructor<?> rowConstructor) {
        throw new UnsupportedOperationException("visit(RowConstructor) not supported");
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        throw new UnsupportedOperationException("visit(RowGetExpression) not supported");
    }

    @Override
    public void visit(OracleHint oracleHint) {
        throw new UnsupportedOperationException("visit(OracleHint) not supported");
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        throw new UnsupportedOperationException("visit(TimeKeyExpression) not supported");
    }

    @Override
    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {
        throw new UnsupportedOperationException("visit(DateTimeLiteralExpression) not supported");
    }

    @Override
    public void visit(NotExpression notExpression) {
        throw new UnsupportedOperationException("visit(NotExpression) not supported");
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        throw new UnsupportedOperationException("visit(NextValExpression) not supported");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        throw new UnsupportedOperationException("visit(CollateExpression) not supported");
    }

    @Override
    public void visit(SimilarToExpression similarToExpression) {
        throw new UnsupportedOperationException("visit(SimilarToExpression) not supported");
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        throw new UnsupportedOperationException("visit(ArrayExpression) not supported");
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        throw new UnsupportedOperationException("visit(ArrayConstructor) not supported");
    }

    @Override
    public void visit(VariableAssignment variableAssignment) {
        throw new UnsupportedOperationException("visit(VariableAssignment) not supported");
    }

    @Override
    public void visit(XMLSerializeExpr xmlSerializeExpr) {
        throw new UnsupportedOperationException("visit(XMLSerializeExpr) not supported");
    }

    @Override
    public void visit(TimezoneExpression timezoneExpression) {
        throw new UnsupportedOperationException("visit(TimezoneExpression) not supported");
    }

    @Override
    public void visit(JsonAggregateFunction jsonAggregateFunction) {
        throw new UnsupportedOperationException("visit(JsonAggregateFunction) not supported");
    }

    @Override
    public void visit(JsonFunction jsonFunction) {
        throw new UnsupportedOperationException("visit(JsonFunction) not supported");
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        throw new UnsupportedOperationException("visit(ConnectByRootOperator) not supported");
    }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        throw new UnsupportedOperationException("visit(OracleNamedFunctionParameter) not supported");
    }

    @Override
    public void visit(AllColumns allColumns) {
        throw new UnsupportedOperationException("visit(AllColumns) not supported");
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        throw new UnsupportedOperationException("visit(AllTableColumns) not supported");
    }

    @Override
    public void visit(AllValue allValue) {
        throw new UnsupportedOperationException("visit(AllValue) not supported");
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        throw new UnsupportedOperationException("visit(IsDistinctExpression) not supported");
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        throw new UnsupportedOperationException("visit(GeometryDistance) not supported");
    }

    @Override
    public void visit(Select select) {
        throw new UnsupportedOperationException("visit(Select) not supported");
    }

    @Override
    public void visit(TranscodingFunction transcodingFunction) {
        throw new UnsupportedOperationException("visit(TranscodingFunction) not supported");
    }

    @Override
    public void visit(TrimFunction trimFunction) {
        throw new UnsupportedOperationException("visit(TrimFunction) not supported");
    }

    @Override
    public void visit(RangeExpression rangeExpression) {
        throw new UnsupportedOperationException("visit(RangeExpression) not supported");
    }

    @Override
    public void visit(TSQLLeftJoin tsqlLeftJoin) {
        throw new UnsupportedOperationException("visit(TSQLLeftJoin) not supported");
    }

    @Override
    public void visit(TSQLRightJoin tsqlRightJoin) {
        throw new UnsupportedOperationException("visit(TSQLRightJoin) not supported");
    }

    @Override
    public void visit(StringValue stringValue) {
        throw new UnsupportedOperationException("visit(StringValue) not supported");
    }

    @Override
    public void visit(Addition addition) {
        throw new UnsupportedOperationException("visit(Addition) not supported");
    }

    @Override
    public void visit(Division division) {
        throw new UnsupportedOperationException("visit(Division) not supported");
    }


}
