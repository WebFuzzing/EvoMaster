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
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;


import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.evomaster.client.java.sql.internal.ConversionHelper.convertToInstant;
import static org.evomaster.client.java.sql.internal.SqlHeuristicsCalculator.*;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;

public class SqlExpressionEvaluator extends ExpressionVisitorAdapter {

    public static final char BITWISE_NOT = '~';
    public static final char MINUS = '-';
    public static final char PLUS = '+';
    private final DataRow dataRow;
    private final SqlNameContext sqlNameContext;
    private final Stack<Truthness> computedTruthnesses = new Stack<>();
    private final Stack<Object> concreteValues = new Stack<>();
    private final TaintHandler taintHandler;

    public SqlExpressionEvaluator(SqlNameContext sqlNameContext, TaintHandler taintHandler, DataRow dataRow) {
        this.sqlNameContext = sqlNameContext;
        this.taintHandler = taintHandler;
        this.dataRow = dataRow;
    }


    public Truthness getEvaluatedTruthness() {
        if (computedTruthnesses.isEmpty()) {
            throw new IllegalStateException("no Truthness was computed");
        }
        return computedTruthnesses.peek();
    }

    private enum BinaryOperator {
        EQUALS_TO,
        NOT_EQUALS_TO,
        GREATER_THAN,
        GREATER_THAN_EQUALS,
        MINOR_THAN,
        MINOR_THAN_EQUALS
    }

    private BinaryOperator getBinaryOperator(ComparisonOperator comparisonOperator) {
        if (comparisonOperator instanceof EqualsTo) {
            return BinaryOperator.EQUALS_TO;
        } else if (comparisonOperator instanceof NotEqualsTo) {
            return BinaryOperator.NOT_EQUALS_TO;
        } else if (comparisonOperator instanceof GreaterThan) {
            return BinaryOperator.GREATER_THAN;
        } else if (comparisonOperator instanceof GreaterThanEquals) {
            return BinaryOperator.GREATER_THAN_EQUALS;
        } else if (comparisonOperator instanceof MinorThan) {
            return BinaryOperator.MINOR_THAN;
        } else if (comparisonOperator instanceof MinorThanEquals) {
            return BinaryOperator.MINOR_THAN_EQUALS;
        } else {
            throw new IllegalArgumentException("Unsupported ComparisonOperator: " + comparisonOperator.getClass().getName());
        }
    }

    private void visitComparisonOperator(ComparisonOperator comparisonOperator) {
        final Object concreteRightValue = concreteValues.pop();
        final Object concreteLeftValue = concreteValues.pop();
        final BinaryOperator binaryOperator = getBinaryOperator(comparisonOperator);
        final Truthness truthness = evaluateTruthnessForComparisonOperator(concreteLeftValue, concreteRightValue, binaryOperator);
        computedTruthnesses.push(truthness);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        super.visit(equalsTo);
        visitComparisonOperator(equalsTo);
    }

    private Truthness evaluateTruthnessForComparisonOperator(Object concreteLeftValue, Object concreteRightValue, BinaryOperator binaryOperator) {
        final Truthness truthness;
        if (concreteLeftValue == null && concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS;
        } else if (concreteLeftValue == null || concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS_BETTER;
        } else {
            final Truthness truthnessOfExpression;
            if (concreteLeftValue instanceof Number && concreteRightValue instanceof Number) {
                truthnessOfExpression = calculateTruthnessForNumberComparison((Number) concreteLeftValue, (Number) concreteRightValue, binaryOperator);
            } else if (concreteRightValue instanceof String && concreteLeftValue instanceof String) {
                truthnessOfExpression = calculateTruthnessForStringComparison((String) concreteLeftValue, (String) concreteRightValue, binaryOperator);
            } else if (concreteLeftValue instanceof Boolean && concreteRightValue instanceof Boolean) {
                truthnessOfExpression = calculateTruthnessForBooleanComparison((Boolean) concreteLeftValue, (Boolean) concreteRightValue, binaryOperator);
            } else if (concreteLeftValue instanceof java.util.Date || concreteRightValue instanceof java.util.Date) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteRightValue), binaryOperator);
            } else if (concreteLeftValue instanceof OffsetDateTime || concreteRightValue instanceof OffsetDateTime) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteRightValue), binaryOperator);
            } else if (concreteLeftValue instanceof OffsetTime || concreteRightValue instanceof OffsetTime) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteLeftValue), binaryOperator);
            } else {
                throw new UnsupportedOperationException("types not supported " + concreteLeftValue.getClass().getName() + " and " + concreteRightValue.getClass().getName());
            }
            if (truthnessOfExpression.isTrue()) {
                truthness = truthnessOfExpression;
            } else {
                truthness = buildScaledTruthness(SqlHeuristicsCalculator.C_BETTER, truthnessOfExpression.getOfTrue());
            }
        }
        return truthness;
    }

    private static Truthness calculateTruthnessForInstantComparison(Instant leftInstant, Instant rightInstant, BinaryOperator binaryOperator) {
        Objects.requireNonNull(leftInstant);
        Objects.requireNonNull(rightInstant);
        final long leftInstantMillis = leftInstant.toEpochMilli();
        final long rightInstantMillis = rightInstant.toEpochMilli();
        return calculateTruthnessForDoubleComparison(leftInstantMillis, rightInstantMillis, binaryOperator);
    }

    private Truthness calculateTruthnessForBooleanComparison(Boolean concreteLeftValue, Boolean concreteRightValue, BinaryOperator binaryOperator) {
        Objects.requireNonNull(concreteLeftValue);
        Objects.requireNonNull(concreteRightValue);

        double leftValueAsDouble = toDouble(concreteLeftValue);
        double rightValueAsDouble = toDouble(concreteRightValue);
        switch (binaryOperator) {
            case EQUALS_TO:
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble);
            case NOT_EQUALS_TO:
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble).invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + binaryOperator);
        }
    }

    private static double toDouble(Boolean booleanValue) {
        return (booleanValue ? 1d : 0d);
    }

    public static Truthness getEqualityTruthness(String a, String b) {
        if (a.equals(b)) {
            return TRUE_TRUTHNESS;
        } else {
            final double base = C;
            final double distance = DistanceHelper.getLeftAlignmentDistance(a, b);
            final double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
            return new Truthness(h, 1d);
        }
    }

    private Truthness calculateTruthnessForStringComparison(String leftString, String rightString, BinaryOperator binaryOperator) {
        Objects.requireNonNull(leftString);
        Objects.requireNonNull(rightString);

        switch (binaryOperator) {
            case EQUALS_TO:
                if (taintHandler != null) {
                    taintHandler.handleTaintForStringEquals(leftString, rightString, false);
                }
                return getEqualityTruthness(leftString, rightString);
            case NOT_EQUALS_TO:
                return getEqualityTruthness(leftString, rightString).invert();
            case GREATER_THAN:
                return leftString.compareTo(rightString) > 0 ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            case GREATER_THAN_EQUALS:
                return leftString.compareTo(rightString) >= 0 ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            case MINOR_THAN:
                return leftString.compareTo(rightString) < 0 ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            case MINOR_THAN_EQUALS:
                return leftString.compareTo(rightString) <= 0 ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;

            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + binaryOperator);
        }
    }

    private static Truthness calculateTruthnessForNumberComparison(Number leftNumber, Number rightNumber, BinaryOperator binaryOperator) {
        Objects.requireNonNull(leftNumber);
        Objects.requireNonNull(rightNumber);
        double leftValueAsDouble = leftNumber.doubleValue();
        double rightValueAsDouble = rightNumber.doubleValue();
        return calculateTruthnessForDoubleComparison(leftValueAsDouble, rightValueAsDouble, binaryOperator);
    }

    private static Truthness calculateTruthnessForDoubleComparison(double leftValueAsDouble, double rightValueAsDouble, BinaryOperator binaryOperator) {
        switch (binaryOperator) {
            case EQUALS_TO:
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble);
            case NOT_EQUALS_TO:
                //a != b => !(a == b)
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble).invert();
            case GREATER_THAN:
                //a > b => b < a
                return getLessThanTruthness(rightValueAsDouble, leftValueAsDouble);
            case MINOR_THAN:
                return getLessThanTruthness(leftValueAsDouble, rightValueAsDouble);
            case MINOR_THAN_EQUALS:
                //a <= b => b >= a => !(b < a)
                return getLessThanTruthness(rightValueAsDouble, leftValueAsDouble).invert();
            case GREATER_THAN_EQUALS:
                //a >= b => ! (a < b)
                return getLessThanTruthness(leftValueAsDouble, rightValueAsDouble).invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + binaryOperator);
        }
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        super.visit(bitwiseRightShift);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
        final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
        final int result = leftValueAsInt >> rightValueAsInt;
        concreteValues.push(result);
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        super.visit(bitwiseLeftShift);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
        final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
        final int result = leftValueAsInt << rightValueAsInt;
        concreteValues.push(result);
    }

    @Override
    public void visit(NullValue nullValue) {
        concreteValues.push(null);
    }

    @Override
    public void visit(Function function) {
        throw new UnsupportedOperationException("Function not supported");
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        super.visit(signedExpression);
        final Number numberWithoutSign = (Number) concreteValues.pop();
        final Number result;
        switch (signedExpression.getSign()) {
            case PLUS: {
                result = numberWithoutSign.doubleValue();
                break;
            }
            case MINUS: {
                result = -(numberWithoutSign.doubleValue());
                break;
            }
            case BITWISE_NOT: {
                result = ~(numberWithoutSign.intValue());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported sign: " + signedExpression.getSign());
            }
        }
        concreteValues.push(result);
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
        double concreteValue = doubleValue.getValue();
        concreteValues.push(concreteValue);
    }

    @Override
    public void visit(LongValue longValue) {
        long concreteValue = longValue.getValue();
        concreteValues.push(concreteValue);
    }

    @Override
    public void visit(HexValue hexValue) {
        String hexString = hexValue.getValue();
        int intValue = Integer.parseInt(hexString.substring(2), 16);
        concreteValues.push(intValue);
    }

    @Override
    public void visit(DateValue dateValue) {
        final java.sql.Date value = dateValue.getValue();
        concreteValues.push(value);
    }

    @Override
    public void visit(TimeValue timeValue) {
        final java.sql.Time value = timeValue.getValue();
        concreteValues.push(value);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        final Timestamp value = timestampValue.getValue();
        concreteValues.push(value);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        super.visit(parenthesis);
    }


    @Override
    public void visit(IntegerDivision integerDivision) {
        super.visit(integerDivision);
        this.visitArithmeticBinaryExpression(integerDivision);
    }

    @Override
    public void visit(Multiplication multiplication) {
        super.visit(multiplication);
        this.visitArithmeticBinaryExpression(multiplication);
    }

    @Override
    public void visit(Subtraction subtraction) {
        super.visit(subtraction);
        this.visitArithmeticBinaryExpression(subtraction);
    }

    @Override
    public void visit(AndExpression andExpression) {
        super.visit(andExpression);
        Truthness rightTruthnessValue = computedTruthnesses.pop();
        Truthness leftTruthnessValue = computedTruthnesses.pop();
        Truthness andTruthness = buildAndAggregationTruthness(leftTruthnessValue, rightTruthnessValue);
        computedTruthnesses.push(andTruthness);
    }

    @Override
    public void visit(OrExpression orExpression) {
        super.visit(orExpression);
        Truthness rightTruthnessValue = computedTruthnesses.pop();
        Truthness leftTruthnessValue = computedTruthnesses.pop();
        Truthness orTruthness = buildOrAggregationTruthness(leftTruthnessValue, rightTruthnessValue);
        computedTruthnesses.push(orTruthness);
    }

    @Override
    public void visit(XorExpression xorExpression) {
        super.visit(xorExpression);
        Truthness rightTruthnessValue = computedTruthnesses.pop();
        Truthness leftTruthnessValue = computedTruthnesses.pop();

        Truthness xorTruthness = buildXorAggregationTruthness(leftTruthnessValue, rightTruthnessValue);
        computedTruthnesses.push(xorTruthness);
    }

    @Override
    public void visit(Between between) {
        super.visit(between);
        Object endRangeValue = this.concreteValues.pop();
        Object startRangeValue = this.concreteValues.pop();
        Object leftExpressionValue = this.concreteValues.pop();

        Truthness startCondition = evaluateTruthnessForComparisonOperator(leftExpressionValue, startRangeValue, BinaryOperator.GREATER_THAN_EQUALS);
        Truthness endCondition = evaluateTruthnessForComparisonOperator(leftExpressionValue, endRangeValue, BinaryOperator.MINOR_THAN_EQUALS);
        Truthness betweenCondition = buildAndAggregationTruthness(startCondition, endCondition);
        this.computedTruthnesses.push(betweenCondition);
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        super.visit(overlapsCondition);
        List<Object> rightRange = (List<Object>) concreteValues.pop();
        List<Object> leftRange = (List<Object>) concreteValues.pop();
        Object leftStart = leftRange.get(0);
        Object leftEnd = leftRange.get(1);
        Object rightStart = rightRange.get(0);
        Object rightEnd = rightRange.get(1);
        Truthness rangeStart = evaluateTruthnessForComparisonOperator(leftStart,rightEnd,BinaryOperator.MINOR_THAN_EQUALS);
        Truthness rangeEnd =  evaluateTruthnessForComparisonOperator(rightStart,leftEnd,BinaryOperator.MINOR_THAN_EQUALS);
        Truthness overlaps = buildAndAggregationTruthness(rangeStart,rangeEnd);
        computedTruthnesses.push(overlaps);
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
        super.visit(isNullExpression);
        final Object concreteLeftValue = concreteValues.pop();
        final Truthness truthness;
        if (isNullExpression.isNot()) {
            truthness = getTruthnessToIsNull(concreteLeftValue).invert();
        } else {
            truthness = getTruthnessToIsNull(concreteLeftValue);
        }
        computedTruthnesses.push(truthness);
    }

    public static Truthness getTruthnessToIsNull(Object concreteValue) {
        final Truthness truthness;
        if (concreteValue == null) {
            truthness = TRUE_TRUTHNESS;
        } else {
            truthness = FALSE_TRUTHNESS;
        }
        return truthness;
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        super.visit(isBooleanExpression);
        final Object leftConcreteValue = concreteValues.pop();
        boolean rightBooleanValue = isBooleanExpression.isNot() ?
                !isBooleanExpression.isTrue() :
                isBooleanExpression.isTrue();

        final Truthness truthness = getTruthnessForIsBooleanExpression(leftConcreteValue, rightBooleanValue);
        computedTruthnesses.push(truthness);
    }

    private static Truthness getTruthnessForIsBooleanExpression(Object leftConcreteValue, boolean rightBooleanValue) {
        final Truthness truthness;
        if (leftConcreteValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS_BETTER;
        } else {
            final boolean leftBoolean = (Boolean) leftConcreteValue;
            if (leftBoolean == rightBooleanValue) {
                truthness = SqlHeuristicsCalculator.TRUE_TRUTHNESS;
            } else {
                truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS;
            }
        }
        return truthness;
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
        super.visit(modulo);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        final double leftValueAsDouble = ((Number) leftConcreteValue).doubleValue();
        final double rightValueAsDouble = ((Number) rightConcreteValue).doubleValue();
        final double result = leftValueAsDouble % rightValueAsDouble;
        concreteValues.push(result);
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
        super.visit(expressionList);
        List<Object> list = Stream.generate(() -> concreteValues.pop())
                .limit(expressionList.size())
                .collect(Collectors.toList());
        Collections.reverse(list);
        concreteValues.push(list);
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
        String dateTimeAsString = dateTimeLiteralExpression.getValue();
        String dateTimeWithoutEnclosingQuotes = SqlStringUtils.removeEnclosingQuotes(dateTimeAsString);
        concreteValues.push(dateTimeWithoutEnclosingQuotes);
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
        String concreteValue = stringValue.getValue();
        concreteValues.push(concreteValue);
    }

    @Override
    public void visit(Addition addition) {
        super.visit(addition);
        this.visitArithmeticBinaryExpression(addition);
    }

    @Override
    public void visit(Division division) {
        super.visit(division);
        this.visitArithmeticBinaryExpression(division);
    }

    private void visitArithmeticBinaryExpression(BinaryExpression binaryExpression) {
        final Number concreteRightValueAsNumber = (Number) concreteValues.pop();
        final Number concreteLeftValueAsNumber = (Number) concreteValues.pop();
        final double concreteLeftValueAsDouble = concreteLeftValueAsNumber.doubleValue();
        final double concreteRightValueAsDouble = concreteRightValueAsNumber.doubleValue();
        final double resultAsDouble;
        if (binaryExpression instanceof Division) {
            resultAsDouble = concreteLeftValueAsDouble / concreteRightValueAsDouble;
        } else if (binaryExpression instanceof Multiplication) {
            resultAsDouble = concreteLeftValueAsDouble * concreteRightValueAsDouble;
        } else if (binaryExpression instanceof Subtraction) {
            resultAsDouble = concreteLeftValueAsDouble - concreteRightValueAsDouble;
        } else if (binaryExpression instanceof Addition) {
            resultAsDouble = concreteLeftValueAsDouble + concreteRightValueAsDouble;
        } else if (binaryExpression instanceof IntegerDivision) {
            resultAsDouble = Math.floor(concreteLeftValueAsDouble / concreteRightValueAsDouble);
        } else {
            throw new UnsupportedOperationException("unsupported binary expression: " + binaryExpression.getClass().getName());
        }
        concreteValues.push(resultAsDouble);
    }


}
