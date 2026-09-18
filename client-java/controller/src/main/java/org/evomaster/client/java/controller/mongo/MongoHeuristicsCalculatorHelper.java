package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.controller.mongo.utils.MongoUtils;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.documentKeys;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.isBsonDocument;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getDistanceBetweenPoints;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getIntegralLongValue;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.*;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO;

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
     * @param actualValue  the input string to be matched against the regular expression
     * @param pattern      the compiled {@link Pattern} representing the regular expression to match
     * @param taintHandler an optional implementation of {@link TaintHandler} to handle taint-related processing;
     *                     may be null if taint handling is not required
     * @return a {@link Truthness} object representing the result of the evaluation,
     * where one component (true or false) is 1 to reflect the match status, and the other
     * captures the approximation in cases of non-exact matches
     */
    static Truthness evaluateRegularExpression(String actualValue, Pattern pattern, TaintHandler taintHandler) {
        final String patternString = pattern.pattern();

        if (taintHandler != null) {
            final int patternFlags = pattern.flags();
            // TODO: tainting should take into account the pattern flags, which can change the matching behavior
            // TODO: regex could be a partial word match (MongoDB $regex) instead of a whole word match (Matcher.matches())
            taintHandler.handleTaintForRegex(actualValue, patternString);
        }


        Matcher matcher = pattern.matcher(actualValue);
        boolean matches = matcher.find();

        if (matches) {
            return TRUE_C;
        } else {
            // TODO this does not take into account pattern flags, which can change the matching behavior
            final int distance = RegexDistanceUtils.getStandardDistance(actualValue, patternString);
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

    Truthness compareNonNullLists(List<?> actualValueAsList, ComparisonOperatorType op, List<?> expectedValueAsList) {

        Objects.requireNonNull(expectedValueAsList);
        Objects.requireNonNull(actualValueAsList);

        final Truthness truthness = evaluateListEquality(expectedValueAsList, actualValueAsList);
        switch (op) {
            case EQUALS_TO:
                return truthness;
            case NOT_EQUALS_TO:
                return truthness.invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + op);
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
            arrayOfTruthnesses[i] = compareNullableValues(actualList.get(i), EQUALS_TO, expectedList.get(i));
        }
        Truthness unscaledTruthness = buildAndAggregationTruthness(arrayOfTruthnesses);
        final Truthness truthness = buildSafeScaledTruthness(unscaledTruthness);
        return truthness;
    }

    List<Truthness> compareDocuments(Object actualValue, ComparisonOperatorType op, Object expectedValue) {

        Objects.requireNonNull(actualValue);
        Objects.requireNonNull(op);
        Objects.requireNonNull(expectedValue);
        if (!isBsonDocument(expectedValue) || !isBsonDocument(actualValue)) {
            throw new IllegalArgumentException("Both expected and actual values must be BSON documents.");
        }

        List<Truthness> truthnesses = new ArrayList<>();
        Set<String> expectedFieldNames = documentKeys(expectedValue);
        Set<String> actualFieldNames = documentKeys(actualValue);
        /**
         * org.bson.Document preserves insertion order. This means that the order of field names
         * in the expected and actual documents may differ even if they contain the same fields.
         */
        Iterator<String> expectedFieldNamesIterator = expectedFieldNames.iterator();
        Iterator<String> actualFieldNamesIterator = actualFieldNames.iterator();
        while (expectedFieldNamesIterator.hasNext() && actualFieldNamesIterator.hasNext()) {
            String expectedFieldName = expectedFieldNamesIterator.next();
            String actualFieldName = actualFieldNamesIterator.next();
            Truthness fieldNameComparisonTruthness = compareNonNullValues(actualFieldName, op, expectedFieldName);
            truthnesses.add(fieldNameComparisonTruthness);
            if (!expectedFieldName.equals(actualFieldName)) {
                return truthnesses;
            }
            Object expectedFieldValue = BsonHelper.getValue(expectedValue, expectedFieldName);
            Object actualFieldValue = BsonHelper.getValue(actualValue, expectedFieldName);
            Truthness fieldValueComparisonTruthness = compareNullableValues(actualFieldValue, op, expectedFieldValue);
            truthnesses.add(fieldValueComparisonTruthness);
            if (!Objects.equals(expectedFieldValue, actualFieldValue)) {
                return truthnesses;
            }
        }
        Truthness fieldTruthness = compareNonNullValues(actualFieldNamesIterator.hasNext(), op, expectedFieldNamesIterator.hasNext());
        truthnesses.add(fieldTruthness);
        return truthnesses;
    }


    Truthness compareNonNullValues(Object actualValue, ComparisonOperatorType op, Object expectedValue) {

        Objects.requireNonNull(expectedValue);
        Objects.requireNonNull(actualValue);

        final Truthness truthnessOfComparison;
        if (expectedValue instanceof Number && actualValue instanceof Number) {
            final Number expectedValueAsNumber = (Number) expectedValue;
            final Number actualNumberAsValue = (Number) actualValue;
            truthnessOfComparison = compareNumberValues(actualNumberAsValue, op, expectedValueAsNumber);

        } else if (expectedValue instanceof String && actualValue instanceof String) {
            String expectedValueAsString = (String) expectedValue;
            String actualValueAsString = (String) actualValue;
            if (taintHandler != null && op == EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(expectedValueAsString, actualValueAsString, false);
            }
            truthnessOfComparison = calculateTruthnessForStringComparison(
                    actualValueAsString, expectedValueAsString, op);

        } else if (expectedValue instanceof Boolean && actualValue instanceof Boolean) {
            int expectedValueAsInt = toIntValue((Boolean) expectedValue);
            int actualValueAsInt = toIntValue((Boolean) actualValue);
            truthnessOfComparison = calculateTruthnessForNumberComparison(
                    actualValueAsInt, expectedValueAsInt, op);

        } else if (expectedValue instanceof List<?> && actualValue instanceof List<?>) {
            final List<?> expectedValueAsList = (List<?>) expectedValue;
            final List<?> actualValueAsList = (List<?>) actualValue;
            truthnessOfComparison = compareNonNullLists(actualValueAsList, op, expectedValueAsList);

        } else if (expectedValue instanceof Date && actualValue instanceof Date) {
            final Instant expectedValueAsInstant = convertToInstant(expectedValue);
            final Instant actualValueAsInstant = convertToInstant(actualValue);
            truthnessOfComparison = calculateTruthnessForInstantComparison(
                    actualValueAsInstant, expectedValueAsInstant, op);

        } else if (BsonHelper.isBsonTimestamp(expectedValue) && BsonHelper.isBsonTimestamp(actualValue)) {
            long expectedValueAsTimestampValue = BsonHelper.getBsonTimestampValue(expectedValue);
            long actualValueAsTimestampValue = BsonHelper.getBsonTimestampValue(actualValue);
            truthnessOfComparison = calculateTruthnessForNumberComparison(actualValueAsTimestampValue, expectedValueAsTimestampValue, op);

        } else if (BsonHelper.isBsonRegularExpression(expectedValue) && BsonHelper.isBsonRegularExpression(actualValue)) {
            String expectedValuePatternAsString = BsonHelper.bsonRegexGetPattern(expectedValue);
            String actualValuePatternAsString = BsonHelper.bsonRegexGetPattern(actualValue);
            truthnessOfComparison = calculateTruthnessForStringComparison(actualValuePatternAsString, expectedValuePatternAsString, op);

        } else if (BsonHelper.isObjectId(expectedValue) || BsonHelper.isObjectId(actualValue)) {
            String expectedValueAsString = expectedValue.toString();
            String actualValueAsString = actualValue.toString();
            if (taintHandler != null && op == EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(expectedValueAsString, actualValueAsString, false);
            }
            truthnessOfComparison = calculateTruthnessForStringComparison(actualValueAsString, expectedValueAsString, op);

        } else if (BsonHelper.isBsonBinary(expectedValue) && BsonHelper.isBsonBinary(actualValue)) {
            byte[] expectedValueAsByteArray = BsonHelper.getBinaryData(expectedValue);
            byte[] actualValueAsByteArray = BsonHelper.getBinaryData(actualValue);

            truthnessOfComparison = compareBinaryData(actualValueAsByteArray, op, expectedValueAsByteArray);
        } else if (BsonHelper.isBsonDocument(expectedValue) && BsonHelper.isBsonDocument(actualValue)) {
            truthnessOfComparison = compareDocumentValues(actualValue, op, expectedValue);
        } else {
            // If both types are supported, but no actual comparison logic is defined,
            // we considered them to be incompatible, therefore the comparison returns true
            // only if the comparison operator is NOT_EQUALS_TO. Otherwise returns false.
            truthnessOfComparison = op == NOT_EQUALS_TO ? TRUE_C : C_FALSE;
        }
        return truthnessOfComparison;
    }

    private Truthness compareDocumentValues(Object actualValue,
                                            ComparisonOperatorType op,
                                            Object expectedValue) {

        Objects.requireNonNull(expectedValue);
        Objects.requireNonNull(op);
        Objects.requireNonNull(actualValue);
        if (!isBsonDocument(expectedValue) || !isBsonDocument(actualValue)) {
            throw new IllegalArgumentException("Both expected and actual values must be BSON documents.");
        }

        List<Truthness> truthnesses = compareDocuments(actualValue, op, expectedValue);
        return buildAndAggregationTruthness(truthnesses.toArray(new Truthness[0]));
    }

    private Truthness compareBinaryData(byte[] expectedValueAsByteArray,
                                        ComparisonOperatorType op,
                                        byte[] actualValueAsByteArray) {

        final Truthness equalityTruthness = getEqualityTruthness(actualValueAsByteArray, expectedValueAsByteArray);
        switch (op) {
            case EQUALS_TO:
                return equalityTruthness;
            case NOT_EQUALS_TO:
                return equalityTruthness.invert();
            case GREATER_THAN:
            case GREATER_THAN_EQUALS:
            case MINOR_THAN:
            case MINOR_THAN_EQUALS:
                // TODO: Must implement comparison operator type for byte[] values. Currently only EQUALS_TO and NOT_EQUALS_TO are supported.
                throw new IllegalArgumentException("Must implement comparison operator type: " + op);
            default:
                throw new IllegalArgumentException("Unknown operator type: " + op);
        }
    }

    static Truthness compareNumberValues(Number actualValueAsNumber, ComparisonOperatorType op, Number expectedValueAsNumber) {
        Objects.requireNonNull(expectedValueAsNumber);
        Objects.requireNonNull(op);
        Objects.requireNonNull(actualValueAsNumber);

        double expectedValueAsDouble = expectedValueAsNumber.doubleValue();
        double actualValueAsDouble = actualValueAsNumber.doubleValue();

        if (Double.isNaN(expectedValueAsDouble) || Double.isNaN(actualValueAsDouble)) {
            // handle case when NaN is involved in the comparison
            switch (op) {
                case EQUALS_TO: {
                    return (Double.isNaN(expectedValueAsDouble) && Double.isNaN(actualValueAsDouble)) ?
                            TRUE_C : C_FALSE;
                }
                case NOT_EQUALS_TO: {
                    return (!Double.isNaN(expectedValueAsDouble) || !Double.isNaN(actualValueAsDouble)) ?
                            TRUE_C : C_FALSE;
                }
                case GREATER_THAN:
                case GREATER_THAN_EQUALS:
                case MINOR_THAN:
                case MINOR_THAN_EQUALS: {
                    return C_FALSE;
                }
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator type: " + op);
            }
        } else {
            // if both values are not NaN, we can use the standard comparison logic
            final Truthness truthnessOfComparison = calculateTruthnessForNumberComparison(actualValueAsNumber, expectedValueAsNumber, op);
            return truthnessOfComparison;
        }
    }

    Truthness compareNullableValues(Object actualValue, ComparisonOperatorType op, Object expectedValue) {
        if (expectedValue == null || actualValue == null) {
            switch (op) {
                case MINOR_THAN_EQUALS:
                case GREATER_THAN_EQUALS:
                case EQUALS_TO:
                    return (expectedValue == null && actualValue == null) ? TRUE_C : C_FALSE;
                case NOT_EQUALS_TO:
                    return (expectedValue == null && actualValue == null) ? C_FALSE : TRUE_C;
                case GREATER_THAN:
                case MINOR_THAN:
                    return C_FALSE;
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator type: " + op);
            }
        } else {
            Truthness valTruthness = compareNonNullValues(actualValue, op, expectedValue
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
                    .map(expectedValue -> compareNullableValues(element, EQUALS_TO, expectedValue
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

    Truthness evaluateMod(Object actualValue, long divisor, long expectedRemainder) {
        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }

        long actualRemainder = ((Number) actualValue).longValue() % divisor;
        Truthness res = getEqualityTruthness(actualRemainder, expectedRemainder);
        return buildSafeScaledTruthness(res);
    }


    Truthness evaluateBitsAllClearOperation(Object actualValue, long bitmask) {
        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) actualValue;
        final OptionalLong integralValue = getIntegralLongValue(number);

        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        Truthness equalityTruthness = getEqualityTruthness(numberOfSetBitsInMaskedValue, 0);
        return buildSafeScaledTruthness(equalityTruthness);
    }

    Truthness evaluateBitsAllSetOperation(Object actualValue, long bitmask) {

        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) actualValue;
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

    Truthness evaluateBitsAnyClearOperation(Object actualValue, long bitmask) {

        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }
        final Number number = (Number) actualValue;
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

    Truthness evaluateBitsAnySetOperation(Object actualValue, long bitmask) {

        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }

        Number number = (Number) actualValue;
        final OptionalLong integralValue = getIntegralLongValue(number);
        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        Truthness lessThanTruthness = getLessThanTruthness(0, numberOfSetBitsInMaskedValue);
        return buildSafeScaledTruthness(lessThanTruthness);
    }

    static Truthness evaluateDistanceBetweenPoints(Object actualValue, double minDistance,
                                                   double maxDistance,
                                                   double longitude,
                                                   double latitude,
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

    Truthness evaluateEquality(Object actualValue, Object expectedValue) {
        Truthness topLevelTruthness = this.compareNullableValues(actualValue, EQUALS_TO, expectedValue);
        if (topLevelTruthness.isTrue()) {
            return topLevelTruthness;
        } else {
            return evaluateWithArrayUnwrapping(actualValue,
                    value -> this.compareNullableValues(
                            value, EQUALS_TO, expectedValue
                    ));
        }
    }

    /**
     * Evaluates a nested structure or a single actualValue using a provided heuristic function.
     * For nested structures that are lists, it aggregates the results of applying the heuristic
     * function to each element in the list. For single values, it directly applies the heuristic.
     *
     * @param actualValue      the actualValue to evaluate, which can either be a single actualValue or a list of values
     * @param elementHeuristic a function to compute the heuristic for each element or the single actualValue
     * @return a Truthness object representing the heuristic of the evaluated input
     */
    Truthness evaluateWithArrayUnwrapping(Object actualValue,
                                          Function<Object, Truthness> elementHeuristic) {
        Objects.requireNonNull(elementHeuristic);

        if (!(actualValue instanceof List<?>)) {
            // actualValue is not an array. Inspect only this actualValue.
            return elementHeuristic.apply(actualValue);
        } else {
            // actualValue is an array. Inspect all elements.
            List<?> actualValueList = (List<?>) actualValue;
            if (actualValueList.isEmpty()) {
                return C_FALSE;
            }
            Truthness orAggregation = buildOrAggregationTruthness(actualValueList.stream()
                    .map(elementHeuristic)
                    .toArray(Truthness[]::new));
            return buildSafeScaledTruthness(orAggregation);
        }
    }

}
