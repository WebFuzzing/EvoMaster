package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.controller.mongo.utils.MongoUtils;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.isBsonDocument;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getDistanceBetweenPoints;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getIntegralLongValue;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;

public class MongoHeuristicsCalculatorHelper {

    // TODO these constants should be replaced by DistanceHelper constants
    public static final double C = 0.1;
    public static final Truthness C_FALSE = new Truthness(C, 1.0);
    // TODO These constants should be refactored by TruthnessUtils constants
    public static final Truthness TRUE_C = new Truthness(1.0, C);

    private final TaintHandler taintHandler;

    public MongoHeuristicsCalculatorHelper(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    static Truthness buildSafeScaledTruthness(double maxOfTrue) {
        if (maxOfTrue == 1.0) {
            return TRUE_C;
        } else {
            return buildScaledTruthness(C, maxOfTrue);
        }
    }


    /**
     * Evaluates whether the given input string matches the provided regular expression pattern.
     * The result is represented as a {@link Truthness} object, which captures both
     * the definitive match result and the closeness to a potential match.
     * Optionally, a {@link TaintHandler} can be used to handle tainting information
     * related to the regular expression processing.
     *
     * @param inputValue   the input string to be matched against the regular expression
     * @param pattern      the compiled {@link Pattern} representing the regular expression to match
     * @param taintHandler an optional implementation of {@link TaintHandler} to handle taint-related processing;
     *                     may be null if taint handling is not required
     * @return a {@link Truthness} object representing the result of the evaluation,
     * where one component (true or false) is 1 to reflect the match status, and the other
     * captures the approximation in cases of non-exact matches
     */
    static Truthness evaluateRegularExpression(String inputValue, Pattern pattern, TaintHandler taintHandler) {
        final String patternString = pattern.pattern();

        if (taintHandler != null) {
            final int patternFlags = pattern.flags();
            // TODO: tainting should take into account the pattern flags, which can change the matching behavior
            // TODO: regex could be a partial word match (MongoDB $regex) instead of a whole word match (Matcher.matches())
            taintHandler.handleTaintForRegex(inputValue, patternString);
        }


        Matcher matcher = pattern.matcher(inputValue);
        boolean matches = matcher.find();

        if (matches) {
            return TRUE_C;
        } else {
            // TODO this does not take into account pattern flags, which can change the matching behavior
            final int distance = RegexDistanceUtils.getStandardDistance(inputValue, patternString);
            // The distance approximation can be zero even when Java's matcher rejects the input
            // (for example, when flags affect line terminators). Keep non-matches strictly false.
            double ofTrue = 1d / (1.1d + distance);
            return buildSafeScaledTruthness(ofTrue);
        }
    }

    static int toIntValue(Boolean actualValue) {
        return actualValue ? 1 : 0;
    }

    static Truthness buildSafeScaledTruthness(Truthness truthness) {
        Objects.requireNonNull(truthness);

        return buildSafeScaledTruthness(truthness.getOfTrue());
    }

    Truthness compareNonNullLists(List<?> leftList, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, List<?> rightList) {
        Objects.requireNonNull(leftList);
        Objects.requireNonNull(rightList);

        final Truthness truthness = evaluateListEquality(leftList, rightList);
        switch (comparisonOperatorType) {
            case EQUALS_TO:
                return truthness;
            case NOT_EQUALS_TO:
                return truthness.invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
        }
    }

    Truthness evaluateListEquality(List<?> actualList, List<?> expectedList) {

        if (actualList.size() != expectedList.size()) {
            return C_FALSE;
        }

        if (actualList.isEmpty() && expectedList.isEmpty()) {
            return TRUE_C;
        }

        Truthness[] arrayOfTruthnesses = new Truthness[actualList.size()];
        for (int i = 0; i < actualList.size(); i++) {
            arrayOfTruthnesses[i] = compareNullableValues(
                    actualList.get(i),
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, expectedList.get(i)
            );
        }
        Truthness unscaledTruthness = buildAndAggregationTruthness(arrayOfTruthnesses);
        final Truthness truthness = buildSafeScaledTruthness(unscaledTruthness);
        return truthness;
    }

    Truthness compareNonNullValues(Object leftValue,
                                   SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType,
                                   Object rightValue) {
        Objects.requireNonNull(leftValue);
        Objects.requireNonNull(rightValue);

        final Truthness truthnessOfComparison;
        if (leftValue instanceof Number && rightValue instanceof Number) {
            final Number actualNumber = (Number) leftValue;
            final Number expectedNumber = (Number) rightValue;
            truthnessOfComparison = compareNumberValues(actualNumber, comparisonOperatorType, expectedNumber);

        } else if (leftValue instanceof String && rightValue instanceof String) {
            String actualString = (String) leftValue;
            String expectedString = (String) rightValue;
            if (taintHandler != null && comparisonOperatorType == SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(actualString, expectedString, false);
            }
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForStringComparison(actualString, expectedString, comparisonOperatorType);

        } else if (leftValue instanceof Boolean && rightValue instanceof Boolean) {
            int actualIntValue = toIntValue((Boolean) leftValue);
            int expectedIntValue = toIntValue((Boolean) rightValue);
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison(
                    actualIntValue, expectedIntValue, comparisonOperatorType);

        } else if (leftValue instanceof List<?> && rightValue instanceof List<?>) {
            truthnessOfComparison = compareNonNullLists((List<?>) leftValue, comparisonOperatorType, (List<?>) rightValue);

        } else if (leftValue instanceof Date && rightValue instanceof Date) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForInstantComparison(convertToInstant(leftValue), convertToInstant(rightValue), comparisonOperatorType);


        } else if (BsonHelper.isBsonTimestamp(leftValue) && BsonHelper.isBsonTimestamp(rightValue)) {
            long actualTimestamp = BsonHelper.getBsonTimestampValue(leftValue);
            long expectedTimestamp = BsonHelper.getBsonTimestampValue(rightValue);
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison(actualTimestamp, expectedTimestamp, comparisonOperatorType);

        } else if (BsonHelper.isObjectId(leftValue) || BsonHelper.isObjectId(rightValue)) {
            String actualString = leftValue.toString();
            String expectedString = rightValue.toString();
            if (taintHandler != null && comparisonOperatorType == SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(actualString, expectedString, false);
            }
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForStringComparison(actualString, expectedString, comparisonOperatorType);

        } else {
            // If both types are supported, but no actual comparison logic is defined,
            // we considered them to be incompatible, therefore the comparison returns true
            // only if the comparison operator is NOT_EQUALS_TO. Otherwise returns false.
            truthnessOfComparison = comparisonOperatorType == SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO ? TRUE_C : C_FALSE;
        }
        return truthnessOfComparison;
    }

    static Truthness compareNumberValues(Number leftNumber, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, Number rightNumber) {
        Objects.requireNonNull(leftNumber);
        Objects.requireNonNull(rightNumber);
        Objects.requireNonNull(comparisonOperatorType);

        double leftValueAsDouble = leftNumber.doubleValue();
        double rightValueAsDouble = rightNumber.doubleValue();

        if (Double.isNaN(leftValueAsDouble) || Double.isNaN(rightValueAsDouble)) {
            // handle case when NaN is involved in the comparison
            switch (comparisonOperatorType) {
                case EQUALS_TO: {
                    return (Double.isNaN(leftValueAsDouble) && Double.isNaN(rightValueAsDouble)) ?
                            TRUE_C : C_FALSE;
                }
                case NOT_EQUALS_TO: {
                    return (!Double.isNaN(leftValueAsDouble) || !Double.isNaN(rightValueAsDouble)) ?
                            TRUE_C : C_FALSE;
                }
                case GREATER_THAN:
                case GREATER_THAN_EQUALS:
                case MINOR_THAN:
                case MINOR_THAN_EQUALS: {
                    return C_FALSE;
                }
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator type: " + comparisonOperatorType);
            }
        } else {
            // if both values are not NaN, we can use the standard comparison logic
            final Truthness truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison(leftNumber, rightNumber, comparisonOperatorType);
            return truthnessOfComparison;
        }
    }

    Truthness compareNullableValues(Object leftValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, Object rightValue) {
        if (leftValue == null || rightValue == null) {
            switch (comparisonOperatorType) {
                case EQUALS_TO:
                    return (leftValue == null && rightValue == null) ? TRUE_C : C_FALSE;
                case NOT_EQUALS_TO:
                    return (leftValue == null && rightValue == null) ? C_FALSE : TRUE_C;
                case GREATER_THAN:
                case GREATER_THAN_EQUALS:
                case MINOR_THAN:
                case MINOR_THAN_EQUALS:
                    return C_FALSE;
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator type: " + comparisonOperatorType);
            }
        } else {
            Truthness valTruthness = compareNonNullValues(rightValue,
                    comparisonOperatorType, leftValue
            );
            return buildSafeScaledTruthness(valTruthness);
        }
    }

    /**
     * Computes the heuristic score for determining if an element is contained in a list.
     * The method evaluates the presence of the given element in the provided list and calculates
     * a heuristic truthness score based on the comparison results.
     *
     * @param element the element whose presence in the list is to be evaluated; can be null.
     * @param list    the list of objects to search; must not be null.
     * @return a Truthness object representing the heuristic score of the element's presence in the list.
     */
    Truthness computeHeuristicContainsElement(Object element, List<?> list) {
        Objects.requireNonNull(list);

        if (list.isEmpty()) {
            return C_FALSE;
        } else {
            Truthness res = buildOrAggregationTruthness(list.stream()
                    .map(expectedValue -> compareNullableValues(expectedValue,
                            SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, element
                    ))
                    .toArray(Truthness[]::new));
            return buildSafeScaledTruthness(res);
        }
    }

    Truthness computeHeuristicInOperation(Object actualValue, List<?> expectedValueList) {
        final Truthness res;
        if (actualValue instanceof List<?>) {
            List<?> actualValueList = (List<?>) actualValue;
            // first we try to match the actualValueList as a whole with any element of the expectedValueList
            Truthness[] arrayOfTruthnesses = expectedValueList.stream()
                    .filter(expectedValueListElement -> expectedValueListElement instanceof List<?>)
                    .map(expectedValueListElement -> (List<?>) expectedValueListElement)
                    .map(expectedValueInnerListElement ->
                            evaluateListEquality(expectedValueInnerListElement, actualValueList))
                    .toArray(Truthness[]::new);
            final Truthness isTheValueListEqualToAnyExpectedValueList;
            if (arrayOfTruthnesses.length > 0) {
                isTheValueListEqualToAnyExpectedValueList = buildOrAggregationTruthness(arrayOfTruthnesses);
            } else {
                isTheValueListEqualToAnyExpectedValueList = C_FALSE;
            }

            if (isTheValueListEqualToAnyExpectedValueList.isFalse()) {
                // if we fail, we try to match each element of the actualValueList with any element of the expectedValueList
                if (actualValueList.isEmpty()) {
                    res = C_FALSE;
                } else {
                    Truthness orAggregation = buildOrAggregationTruthness(actualValueList.stream()
                            .map(actualValueListElement -> computeHeuristicContainsElement(actualValueListElement, expectedValueList))
                            .toArray(Truthness[]::new));
                    res = buildSafeScaledTruthness(orAggregation);
                }
            } else {
                res = isTheValueListEqualToAnyExpectedValueList;
            }
        } else {
            res = computeHeuristicContainsElement(actualValue, expectedValueList);
        }
        return res;
    }

    Truthness evaluateMod(long divisor, long expectedRemainder, Object actualValue) {
        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }

        long actualRemainder = ((Number) actualValue).longValue() % divisor;
        Truthness res = getEqualityTruthness(actualRemainder, expectedRemainder);
        return buildSafeScaledTruthness(res);
    }


    Truthness evaluateBitsAllClearOperation(long bitmask,
                                            Object value) {
        if (!(value instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) value;
        final OptionalLong integralValue = getIntegralLongValue(number);

        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        Truthness equalityTruthness = getEqualityTruthness(numberOfSetBitsInMaskedValue, 0);
        return buildSafeScaledTruthness(equalityTruthness);
    }

    Truthness evaluateBitsAllSetOperation(long bitmask,
                                          Object value) {

        if (!(value instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) value;
        final OptionalLong integralValue = getIntegralLongValue(number);
        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        final int numberOfBitsInMask = Long.bitCount(bitmask);

        Truthness equalityTruthness = getEqualityTruthness(numberOfSetBitsInMaskedValue, numberOfBitsInMask);
        return buildSafeScaledTruthness(equalityTruthness);
    }

    Truthness evaluateBitsAnyClearOperation(long bitmask,
                                            Object value) {

        if (!(value instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) value;
        final OptionalLong integralValue = getIntegralLongValue(number);
        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        final int numberOfBitsInMask = Long.bitCount(bitmask);

        Truthness lessThanTruthness = getLessThanTruthness(numberOfSetBitsInMaskedValue, numberOfBitsInMask);
        return buildSafeScaledTruthness(lessThanTruthness);
    }

    Truthness evaluateBitsAnySetOperation(long bitmask,
                                          Object value) {

        if (!(value instanceof Number)) {
            return C_FALSE;
        }

        Number number = (Number) value;
        final OptionalLong integralValue = getIntegralLongValue(number);
        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        Truthness lessThanTruthness = getLessThanTruthness(0, numberOfSetBitsInMaskedValue);
        return buildSafeScaledTruthness(lessThanTruthness);
    }

    static Truthness evaluateDistanceBetweenPoints(double minDistance,
                                                   double maxDistance,
                                                   double longitude,
                                                   double latitude,
                                                   Object actualValue,
                                                   MongoUtils.GeoSpatialModel geoSpatialModel) {

        double x1 = geoSpatialModel == SPHERICAL ? Math.toRadians(longitude) : longitude;
        double y1 = geoSpatialModel == SPHERICAL ? Math.toRadians(latitude) : latitude;
        double x2;
        double y2;

    /*
      GeoJSON Point in document.
      type key is case-sensitive.
      (https://datatracker.ietf.org/doc/html/rfc7946#section-1.4)
     */
        if (isBsonDocument(actualValue)
                && GeoJsonUtils.isGeoJsonPoint(actualValue)) {
            GeoJsonPoint geoJsonPoint = GeoJsonUtils.toGeoJsonPoint(actualValue);
            x2 = geoSpatialModel == SPHERICAL
                    ? Math.toRadians(geoJsonPoint.getLongitude())
                    : geoJsonPoint.getLongitude();
            y2 = geoSpatialModel == SPHERICAL
                    ? Math.toRadians(geoJsonPoint.getLatitude())
                    : geoJsonPoint.getLatitude();
        } else {
            return C_FALSE;
        }
        double distanceBetweenPoints = getDistanceBetweenPoints(x1, y1, x2, y2, geoSpatialModel);

        if (minDistance <= distanceBetweenPoints
                && distanceBetweenPoints <= maxDistance) {
            return TRUE_C;
        }

        return (distanceBetweenPoints > maxDistance)
                ? getEqualityTruthness(distanceBetweenPoints, maxDistance)
                : getEqualityTruthness(distanceBetweenPoints, minDistance);
    }

}
