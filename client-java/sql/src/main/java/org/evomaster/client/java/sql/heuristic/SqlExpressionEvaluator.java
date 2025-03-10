package org.evomaster.client.java.sql.heuristic;

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
import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.internal.*;


import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;
import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.*;
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

    private enum ComparisonOperatorType {
        EQUALS_TO,
        NOT_EQUALS_TO,
        GREATER_THAN,
        GREATER_THAN_EQUALS,
        MINOR_THAN,
        MINOR_THAN_EQUALS
    }

    private enum ArithmeticOperationType {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        INTEGER_DIVISION
    }

    private static ComparisonOperatorType toComparisonOperatorType(ComparisonOperator comparisonOperator) {
        if (comparisonOperator instanceof EqualsTo) {
            return ComparisonOperatorType.EQUALS_TO;
        } else if (comparisonOperator instanceof NotEqualsTo) {
            return ComparisonOperatorType.NOT_EQUALS_TO;
        } else if (comparisonOperator instanceof GreaterThan) {
            return ComparisonOperatorType.GREATER_THAN;
        } else if (comparisonOperator instanceof GreaterThanEquals) {
            return ComparisonOperatorType.GREATER_THAN_EQUALS;
        } else if (comparisonOperator instanceof MinorThan) {
            return ComparisonOperatorType.MINOR_THAN;
        } else if (comparisonOperator instanceof MinorThanEquals) {
            return ComparisonOperatorType.MINOR_THAN_EQUALS;
        } else {
            throw new IllegalArgumentException("Unsupported ComparisonOperator: " + comparisonOperator.getClass().getName());
        }
    }

    private void visitComparisonOperator(ComparisonOperator comparisonOperator) {
        final Object concreteRightValue = concreteValues.pop();
        final Object concreteLeftValue = concreteValues.pop();
        final ComparisonOperatorType comparisonOperatorType = toComparisonOperatorType(comparisonOperator);
        final Truthness truthness = evaluateTruthnessForComparisonOperator(concreteLeftValue, concreteRightValue, comparisonOperatorType);
        computedTruthnesses.push(truthness);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        super.visit(equalsTo);
        visitComparisonOperator(equalsTo);
    }

    private Truthness evaluateTruthnessForComparisonOperator(Object concreteLeftValue, Object concreteRightValue, ComparisonOperatorType comparisonOperatorType) {
        final Truthness truthness;
        if (concreteLeftValue == null && concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS;
        } else if (concreteLeftValue == null || concreteRightValue == null) {
            truthness = SqlHeuristicsCalculator.FALSE_TRUTHNESS_BETTER;
        } else {
            final Truthness truthnessOfExpression;
            if (concreteLeftValue instanceof Number && concreteRightValue instanceof Number) {
                truthnessOfExpression = calculateTruthnessForNumberComparison((Number) concreteLeftValue, (Number) concreteRightValue, comparisonOperatorType);
            } else if (concreteRightValue instanceof String && concreteLeftValue instanceof String) {
                truthnessOfExpression = calculateTruthnessForStringComparison((String) concreteLeftValue, (String) concreteRightValue, comparisonOperatorType);
            } else if (concreteLeftValue instanceof Boolean && concreteRightValue instanceof Boolean) {
                truthnessOfExpression = calculateTruthnessForBooleanComparison((Boolean) concreteLeftValue, (Boolean) concreteRightValue, comparisonOperatorType);
            } else if (concreteLeftValue instanceof java.util.Date || concreteRightValue instanceof java.util.Date) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteRightValue), comparisonOperatorType);
            } else if (concreteLeftValue instanceof OffsetDateTime || concreteRightValue instanceof OffsetDateTime) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteRightValue), comparisonOperatorType);
            } else if (concreteLeftValue instanceof OffsetTime || concreteRightValue instanceof OffsetTime) {
                truthnessOfExpression = calculateTruthnessForInstantComparison(convertToInstant(concreteLeftValue), convertToInstant(concreteLeftValue), comparisonOperatorType);
            } else if (concreteLeftValue instanceof Object[] && concreteRightValue instanceof Object[]) {
                truthnessOfExpression = calculateTruthnessForArrayComparison((Object[]) concreteLeftValue, (Object[]) concreteRightValue, comparisonOperatorType);
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

    private static Truthness calculateTruthnessForInstantComparison(Instant leftInstant, Instant rightInstant, ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(leftInstant);
        Objects.requireNonNull(rightInstant);
        final long leftInstantMillis = leftInstant.toEpochMilli();
        final long rightInstantMillis = rightInstant.toEpochMilli();
        return calculateTruthnessForDoubleComparison(leftInstantMillis, rightInstantMillis, comparisonOperatorType);
    }

    private static Truthness calculateTruthnessForBooleanComparison(Boolean concreteLeftValue, Boolean concreteRightValue, ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(concreteLeftValue);
        Objects.requireNonNull(concreteRightValue);

        double leftValueAsDouble = toDouble(concreteLeftValue);
        double rightValueAsDouble = toDouble(concreteRightValue);
        switch (comparisonOperatorType) {
            case EQUALS_TO:
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble);
            case NOT_EQUALS_TO:
                return TruthnessUtils.getEqualityTruthness(leftValueAsDouble, rightValueAsDouble).invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
        }
    }

    private Truthness calculateTruthnessForArrayComparison(Object[] leftArray, Object[] rightArray, ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(leftArray);
        Objects.requireNonNull(rightArray);

        boolean leftArrayHasNullValue = Stream.of(leftArray).anyMatch(Objects::isNull);
        boolean rightArrayHasNullValue = Stream.of(rightArray).anyMatch(Objects::isNull);

        if (leftArrayHasNullValue || rightArrayHasNullValue) {
            return FALSE_TRUTHNESS;
        } else {
            final List<Object> leftList = Arrays.asList(leftArray);
            final List<Object> rightList = Arrays.asList(rightArray);
            final Truthness truthness;
            if (rightList.size() != leftList.size()) {
                truthness = FALSE_TRUTHNESS;
            } else {
                Truthness[] truthnesses = new Truthness[leftList.size()];
                for (int i = 0; i < leftList.size(); i++) {
                    truthnesses[i] = evaluateTruthnessForComparisonOperator(leftList.get(i), rightList.get(i), ComparisonOperatorType.EQUALS_TO);
                }
                truthness = buildAndAggregationTruthness(truthnesses);
            }
            switch (comparisonOperatorType) {
                case EQUALS_TO:
                    return truthness;
                case NOT_EQUALS_TO:
                    return truthness.invert();
                default:
                    throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
            }
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

    private Truthness calculateTruthnessForStringComparison(String leftString, String rightString, ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(leftString);
        Objects.requireNonNull(rightString);

        switch (comparisonOperatorType) {
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
                throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
        }
    }

    private static Truthness calculateTruthnessForNumberComparison(Number leftNumber, Number rightNumber, ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(leftNumber);
        Objects.requireNonNull(rightNumber);

        double leftValueAsDouble = leftNumber.doubleValue();
        double rightValueAsDouble = rightNumber.doubleValue();
        return calculateTruthnessForDoubleComparison(leftValueAsDouble, rightValueAsDouble, comparisonOperatorType);
    }

    private static Truthness calculateTruthnessForDoubleComparison(double leftValueAsDouble, double rightValueAsDouble, ComparisonOperatorType comparisonOperatorType) {
        switch (comparisonOperatorType) {
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
                throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
        }
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {
        super.visit(bitwiseRightShift);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
            final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
            final int result = leftValueAsInt >> rightValueAsInt;
            concreteValues.push(result);
        }
    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {
        super.visit(bitwiseLeftShift);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
            final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
            final int result = leftValueAsInt << rightValueAsInt;
            concreteValues.push(result);
        }
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
        Object expressionValue = concreteValues.pop();
        if (expressionValue == null) {
            concreteValues.push(null);
        } else {
            final Number numberWithoutSign = (Number) expressionValue;
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

        if (leftExpressionValue == null || startRangeValue == null || endRangeValue == null) {
            this.computedTruthnesses.push(FALSE_TRUTHNESS_BETTER);
        } else {
            Truthness startCondition = evaluateTruthnessForComparisonOperator(leftExpressionValue, startRangeValue, ComparisonOperatorType.GREATER_THAN_EQUALS);
            Truthness endCondition = evaluateTruthnessForComparisonOperator(leftExpressionValue, endRangeValue, ComparisonOperatorType.MINOR_THAN_EQUALS);
            Truthness betweenCondition = buildAndAggregationTruthness(startCondition, endCondition);
            this.computedTruthnesses.push(betweenCondition);
        }
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
        Truthness rangeStart = evaluateTruthnessForComparisonOperator(leftStart, rightEnd, ComparisonOperatorType.MINOR_THAN_EQUALS);
        Truthness rangeEnd = evaluateTruthnessForComparisonOperator(rightStart, leftEnd, ComparisonOperatorType.MINOR_THAN_EQUALS);
        Truthness overlaps = buildAndAggregationTruthness(rangeStart, rangeEnd);
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
        super.visit(inExpression);
        Object rightValues = concreteValues.pop();
        Object leftValue = concreteValues.pop();
        final Truthness truthness;
        if (leftValue == null || rightValues == null) {
            truthness = FALSE_TRUTHNESS_BETTER;
        } else {
            final List<Object> rightValuesList = (List<Object>) rightValues;
            final Truthness[] truthnesses = rightValuesList.stream()
                    .map(rightValue -> evaluateTruthnessForComparisonOperator(leftValue, rightValue, ComparisonOperatorType.EQUALS_TO))
                    .toArray(Truthness[]::new);
            final Truthness inTruthness = buildOrAggregationTruthness(truthnesses);
            if (inExpression.isNot()) {
                truthness = inTruthness.invert();
            } else {
                truthness = inTruthness;
            }
        }
        computedTruthnesses.push(truthness);
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
        super.visit(likeExpression);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            computedTruthnesses.push(FALSE_TRUTHNESS_BETTER);
        } else {
            final String string = String.valueOf(leftConcreteValue);
            final String likePattern = String.valueOf(rightConcreteValue);
            final String javaRegexPattern = RegexSharedUtils.translateSqlLikePattern(likePattern);
            boolean matches = string.matches(javaRegexPattern);
            Truthness truthness = matches ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            if (likeExpression.isNot()) {
                truthness = truthness.invert();
            }
            computedTruthnesses.push(truthness);
        }
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
        super.visit(concat);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            String result = String.valueOf(leftConcreteValue) + String.valueOf(rightConcreteValue);
            concreteValues.push(result);
        }
    }

    @Override
    public void visit(Matches matches) {
        // TODO: Implement the @@ operator
        throw new UnsupportedOperationException("visit(Matches) not supported");
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        super.visit(bitwiseAnd);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
            final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
            final int result = leftValueAsInt & rightValueAsInt;
            concreteValues.push(result);
        }
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        super.visit(bitwiseOr);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
            final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
            final int result = leftValueAsInt | rightValueAsInt;
            concreteValues.push(result);
        }
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        super.visit(bitwiseXor);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final int leftValueAsInt = ((Number) leftConcreteValue).intValue();
            final int rightValueAsInt = ((Number) rightConcreteValue).intValue();
            final int result = leftValueAsInt ^ rightValueAsInt;
            concreteValues.push(result);
        }
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
        if (leftConcreteValue == null || rightConcreteValue == null) {
            concreteValues.push(null);
        } else {
            final double leftValueAsDouble = ((Number) leftConcreteValue).doubleValue();
            final double rightValueAsDouble = ((Number) rightConcreteValue).doubleValue();
            final double result = leftValueAsDouble % rightValueAsDouble;
            concreteValues.push(result);
        }
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
        super.visit(regExpMatchOperator);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            computedTruthnesses.push(FALSE_TRUTHNESS);
        } else {
            RegExpMatchOperatorType operatorType = regExpMatchOperator.getOperatorType();
            String string = leftConcreteValue.toString();
            String posixPattern = rightConcreteValue.toString();
            final boolean caseSensitive;
            final boolean negate;
            switch (operatorType) {
                case MATCH_CASESENSITIVE: {
                    // case for '~'
                    caseSensitive = true;
                    negate = false;
                }
                break;
                case NOT_MATCH_CASESENSITIVE: {
                    // case for '!~'
                    caseSensitive = true;
                    negate = true;
                }
                break;
                case MATCH_CASEINSENSITIVE: {
                    // case for '*~'
                    caseSensitive = false;
                    negate = false;
                }
                break;
                case NOT_MATCH_CASEINSENSITIVE: {
                    // case for '!~*'
                    caseSensitive = false;
                    negate = true;
                }
                break;
                default:
                    throw new IllegalArgumentException("Unsupported operator type: " + operatorType);
            }
            String javaRegexPattern = RegexSharedUtils.translatePostgresqlPosix(posixPattern, caseSensitive);
            boolean matches = string.matches(javaRegexPattern);
            Truthness truthness = matches ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            if (negate) {
                truthness = truthness.invert();
            }
            computedTruthnesses.push(truthness);
        }
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
        List<Object> list = Stream.generate(concreteValues::pop)
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
        super.visit(notExpression);
        Truthness truthness = computedTruthnesses.pop();
        Truthness notTruthness = truthness.invert();
        computedTruthnesses.push(notTruthness);
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
        super.visit(similarToExpression);
        Object rightConcreteValue = concreteValues.pop();
        Object leftConcreteValue = concreteValues.pop();
        if (leftConcreteValue == null || rightConcreteValue == null) {
            computedTruthnesses.push(FALSE_TRUTHNESS);
        } else {
            String string = leftConcreteValue.toString();
            String similarToPattern = rightConcreteValue.toString();
            String javaRegexPattern = RegexSharedUtils.translateSqlSimilarToPattern(similarToPattern);
            boolean matches = string.matches(javaRegexPattern);
            Truthness truthness = matches ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
            if (similarToExpression.isNot()) {
                truthness = truthness.invert();
            }
            computedTruthnesses.push(truthness);
        }
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        super.visit(arrayExpression);

        Number sqlStopIndex;
        if (arrayExpression.getStopIndexExpression() != null) {
            sqlStopIndex = (Number) concreteValues.pop();
        } else {
            sqlStopIndex = null;
        }

        Number sqlStartIndex;
        if (arrayExpression.getStartIndexExpression() != null) {
            sqlStartIndex = (Number) concreteValues.pop();
        } else {
            sqlStartIndex = 1;
        }

        final Number sqlIndex;
        if (arrayExpression.getIndexExpression() != null) {
            sqlIndex = (Number) concreteValues.pop();
        } else {
            sqlIndex = null;
        }

        final Object[] objectArray = (Object[]) concreteValues.pop();

        if (sqlStopIndex == null) {
            sqlStopIndex = objectArray.length;
        }
        int startIndex = toJavaIndex(sqlStartIndex);
        int stopIndex = toJavaIndex(sqlStopIndex);

        Object[] subArray = Arrays.copyOfRange(objectArray, startIndex, stopIndex + 1);

        if (sqlIndex == null) {
            concreteValues.push(subArray);
        } else {
            final int index = toJavaIndex(sqlIndex);
            final Object item = subArray[index];
            concreteValues.push(item);
        }
    }

    private static int toJavaIndex(Number sqlStartIndex) {
        return sqlStartIndex.intValue() - 1;
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        super.visit(arrayConstructor);
        List<Object> list = Stream.generate(concreteValues::pop)
                .limit(arrayConstructor.getExpressions().size())
                .collect(Collectors.toList());
        Collections.reverse(list);
        concreteValues.push(list.toArray());
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

    private static ArithmeticOperationType toArithmeticOperationType(BinaryExpression binaryExpression) {
        if (binaryExpression instanceof Addition) {
            return ArithmeticOperationType.ADDITION;
        } else if (binaryExpression instanceof Subtraction) {
            return ArithmeticOperationType.SUBTRACTION;
        } else if (binaryExpression instanceof Multiplication) {
            return ArithmeticOperationType.MULTIPLICATION;
        } else if (binaryExpression instanceof Division) {
            return ArithmeticOperationType.DIVISION;
        } else if (binaryExpression instanceof IntegerDivision) {
            return ArithmeticOperationType.INTEGER_DIVISION;
        } else {
            throw new IllegalArgumentException("Unsupported BinaryExpression: " + binaryExpression.getClass().getName());
        }
    }

    private void visitArithmeticBinaryExpression(BinaryExpression binaryExpression) {
        final Object concreteRightValue = concreteValues.pop();
        final Object concreteLeftValue = concreteValues.pop();
        if (concreteLeftValue == null || concreteRightValue == null) {
            concreteValues.push(null);
        } else {
            final ArithmeticOperationType arithmeticOperationType = toArithmeticOperationType(binaryExpression);
            if (concreteLeftValue instanceof Number && concreteRightValue instanceof Number) {
                final Number leftNumber = (Number) concreteLeftValue;
                final Number rightNumber = (Number) concreteRightValue;
                final double resultAsDouble = computeNumberArithmeticOperation(leftNumber, rightNumber, arithmeticOperationType);
                concreteValues.push(resultAsDouble);
            } else if (concreteLeftValue instanceof java.sql.Date && concreteRightValue instanceof java.sql.Date) {
                final java.sql.Date leftDate = (java.sql.Date) concreteLeftValue;
                final java.sql.Date rightDate = (java.sql.Date) concreteRightValue;
                final java.sql.Date resultDate = computeDateArithmeticOperation(leftDate, rightDate, arithmeticOperationType);
                concreteValues.push(resultDate);
            }
        }
    }

    private static java.sql.Date computeDateArithmeticOperation(java.sql.Date leftDate, java.sql.Date rightDate, ArithmeticOperationType arithmeticOperationType) {
        Objects.requireNonNull(leftDate);
        Objects.requireNonNull(rightDate);

        final long leftTime = leftDate.getTime();
        final long rightTime = rightDate.getTime();
        final long resultTime;

        switch (arithmeticOperationType) {
            case SUBTRACTION:
                resultTime = leftTime - rightTime;
                break;
            case ADDITION:
                resultTime = leftTime + rightTime;
                break;
            default:
                throw new IllegalArgumentException("Unsupported ArithmeticOperationType: " + arithmeticOperationType);
        }
        final java.sql.Date resultDate = new java.sql.Date(resultTime);
        return resultDate;
    }

    private static double computeNumberArithmeticOperation(Number leftNumber, Number rightNumber, ArithmeticOperationType arithmeticOperationType) {
        Objects.requireNonNull(leftNumber);
        Objects.requireNonNull(rightNumber);

        final double leftDoubleValue = leftNumber.doubleValue();
        final double rightDoubleValue = rightNumber.doubleValue();
        final double resultAsDouble;
        switch (arithmeticOperationType) {
            case ADDITION:
                resultAsDouble = leftDoubleValue + rightDoubleValue;
                break;
            case SUBTRACTION:
                resultAsDouble = leftDoubleValue - rightDoubleValue;
                break;
            case MULTIPLICATION:
                resultAsDouble = leftDoubleValue * rightDoubleValue;
                break;
            case DIVISION:
                resultAsDouble = leftDoubleValue / rightDoubleValue;
                break;
            case INTEGER_DIVISION:
                resultAsDouble = Math.floor(leftDoubleValue / rightDoubleValue);
                break;
            default:
                throw new IllegalArgumentException("Unsupported ArithmeticOperationType: " + arithmeticOperationType);
        }
        return resultAsDouble;
    }
}
