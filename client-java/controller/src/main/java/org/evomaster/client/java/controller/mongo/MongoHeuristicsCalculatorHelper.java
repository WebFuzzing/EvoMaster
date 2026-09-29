package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonGeometry;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonGeometryIntersection;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.controller.mongo.utils.MongoUtils;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getValue;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getDistanceBetweenPoints;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.getIntegralLongValue;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.*;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO;
import static org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO;

public class MongoHeuristicsCalculatorHelper {

    private final TaintHandler taintHandler;

    public MongoHeuristicsCalculatorHelper(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
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
    public static Truthness evaluateRegularExpression(String actualValue, Pattern pattern, TaintHandler taintHandler) {
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

    private static int toIntValue(Boolean actualValue) {
        return actualValue ? 1 : 0;
    }

    private Truthness compareNonNullLists(List<?> actualValueAsList, ComparisonOperatorType op, List<?> expectedValueAsList) {

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


    /**
     * Computes how close a (possibly dotted) field path is to exist in the given value.
     * The path is followed as long as its fieldPathSegments exist; the first segment that is not
     * found is compared against the field names available at that level.
     */
    public Truthness evaluateExists(Object currentValue, String[] fieldPathSegments, int index) {
        final String currentSegment = fieldPathSegments[index];
        if (isBsonDocument(currentValue)) {
            if (index == fieldPathSegments.length - 1 || !documentContainsField(currentValue, currentSegment)) {
                return evaluateExistsFieldName(currentValue, currentSegment);
            }
            return evaluateExists(getValue(currentValue, currentSegment), fieldPathSegments, index + 1);
        } else if (currentValue instanceof List<?>) {
            // When a segment is applied to an array, MongoDB's dot notation is ambiguous: a numeric
            // segment such as "0" in "a.0" may denote either the element at that position of the
            // array, or a field literally named "0" of the sub-documents held by the array.
            // The path exists if any of these interpretations reaches a value, so a Truthness is
            // collected for each of them and they are OR-aggregated. For example:
            //   {a: [10, 20]}      "a.1"   exists as an array index only
            //   {a: [{"1": "x"}]}  "a.1"   exists as a field name only (index 1 is out of bounds)
            //   {a: [{b: 1}]}      "a.0.b" exists as an array index, the field name "0" is missing
            // A failed interpretation only contributes a false Truthness, which cannot turn the
            // OR-aggregation into true, although it may still provide some gradient.
            final List<?> list = (List<?>) currentValue;
            final List<Truthness> truthnesses = new ArrayList<>();

            final OptionalInt arrayIndexOpt = FieldPathResolver.parseAsArrayIndex(currentSegment);
            if (arrayIndexOpt.isPresent()) {
                final int arrayIndex = arrayIndexOpt.getAsInt();
                if (arrayIndex >= 0 && arrayIndex < list.size()) {
                    // Array index interpretation: the segment is consumed by indexing into the array
                    truthnesses.add(index == fieldPathSegments.length - 1
                            ? TRUE_C
                            : evaluateExists(list.get(arrayIndex), fieldPathSegments, index + 1));
                }
            }
            for (Object element : list) {
                // Field name interpretation: the array is traversed implicitly, without consuming
                // the segment, which is then looked up in each sub-document of the array.
                // Nested arrays are not traversed implicitly.
                if (isBsonDocument(element)) {
                    truthnesses.add(evaluateExists(element, fieldPathSegments, index));
                }
            }
            if (truthnesses.isEmpty()) {
                return C_FALSE;
            }
            return buildSafeScaledTruthness(buildOrAggregationTruthness(truthnesses.toArray(new Truthness[0])));
        } else {
            return C_FALSE;
        }
    }


    /**
     * Evaluates whether the specified field name exists in the given document.
     *
     * @param document
     * @param expectedFieldName
     * @return
     */
    private Truthness evaluateExistsFieldName(Object document, String expectedFieldName) {
        Objects.requireNonNull(expectedFieldName);
        if (expectedFieldName.contains(FieldPathResolver.FIELD_PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Field path must not contain the separator: " + FieldPathResolver.FIELD_PATH_SEPARATOR);
        }
        Set<String> actualFieldNames = documentKeys(document);
        if (actualFieldNames.isEmpty()) {
            return C_FALSE;
        }
        Truthness orTruthness = buildOrAggregationTruthness(actualFieldNames.stream()
                .map(actualFieldName ->
                        compareNonNullValues(actualFieldName, EQUALS_TO, expectedFieldName))
                .toArray(Truthness[]::new));
        return buildSafeScaledTruthness(orTruthness);
    }


    private Truthness evaluateListEquality(List<?> actualList, List<?> expectedList) {

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

    private List<Truthness> compareDocuments(Object actualValue, ComparisonOperatorType op, Object expectedValue) {

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


    /**
     * Compares two non-null values using the specified comparison operator.
     *
     * @param actualValue the actual value to compare, which must not be null
     * @param op the comparison operator to use
     * @param expectedValue the expected value to compare against, which must not be null
     * @return a Truthness object representing the result of the comparison
     */
    public Truthness compareNonNullValues(Object actualValue, ComparisonOperatorType op, Object expectedValue) {
        Objects.requireNonNull(actualValue);
        Objects.requireNonNull(expectedValue);

        final Truthness truthnessOfComparison;
        if (actualValue instanceof Number && expectedValue instanceof Number) {
            final Number expectedValueAsNumber = (Number) expectedValue;
            final Number actualNumberAsValue = (Number) actualValue;
            truthnessOfComparison = compareNumberValues(actualNumberAsValue, op, expectedValueAsNumber);

        } else if (actualValue instanceof String && expectedValue instanceof String) {
            String expectedValueAsString = (String) expectedValue;
            String actualValueAsString = (String) actualValue;
            if (taintHandler != null && op == EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(expectedValueAsString, actualValueAsString, false);
            }
            truthnessOfComparison = calculateTruthnessForStringComparison(
                    actualValueAsString, expectedValueAsString, op);

        } else if (actualValue instanceof Boolean && expectedValue instanceof Boolean) {
            int expectedValueAsInt = toIntValue((Boolean) expectedValue);
            int actualValueAsInt = toIntValue((Boolean) actualValue);
            truthnessOfComparison = calculateTruthnessForNumberComparison(
                    actualValueAsInt, expectedValueAsInt, op);

        } else if (actualValue instanceof List<?> && expectedValue instanceof List<?>) {
            final List<?> expectedValueAsList = (List<?>) expectedValue;
            final List<?> actualValueAsList = (List<?>) actualValue;
            truthnessOfComparison = compareNonNullLists(actualValueAsList, op, expectedValueAsList);

        } else if (actualValue instanceof Date && expectedValue instanceof Date) {
            final Instant expectedValueAsInstant = convertToInstant(expectedValue);
            final Instant actualValueAsInstant = convertToInstant(actualValue);
            truthnessOfComparison = calculateTruthnessForInstantComparison(
                    actualValueAsInstant, expectedValueAsInstant, op);

        } else if (BsonHelper.isBsonTimestamp(actualValue) && BsonHelper.isBsonTimestamp(expectedValue)) {
            long expectedValueAsTimestampValue = BsonHelper.getBsonTimestampValue(expectedValue);
            long actualValueAsTimestampValue = BsonHelper.getBsonTimestampValue(actualValue);
            truthnessOfComparison = calculateTruthnessForNumberComparison(actualValueAsTimestampValue, expectedValueAsTimestampValue, op);

        } else if (BsonHelper.isBsonRegularExpression(actualValue) && BsonHelper.isBsonRegularExpression(expectedValue)) {
            String expectedValuePatternAsString = BsonHelper.bsonRegexGetPattern(expectedValue);
            String actualValuePatternAsString = BsonHelper.bsonRegexGetPattern(actualValue);
            truthnessOfComparison = calculateTruthnessForStringComparison(actualValuePatternAsString, expectedValuePatternAsString, op);

        } else if (BsonHelper.isObjectId(actualValue) && BsonHelper.isObjectId(expectedValue)) {

            byte[] actualValueAsByteArray = BsonHelper.toByteArray(actualValue);
            byte[] expectedValueAsByteArray = BsonHelper.toByteArray(expectedValue);
            truthnessOfComparison = compareBinaryData(actualValueAsByteArray, op, expectedValueAsByteArray);

        } else if (BsonHelper.isBsonBinary(actualValue) && BsonHelper.isBsonBinary(expectedValue)) {

            byte[] actualValueAsByteArray = BsonHelper.getBinaryData(actualValue);
            byte[] expectedValueAsByteArray = BsonHelper.getBinaryData(expectedValue);
            truthnessOfComparison = compareBinaryData(actualValueAsByteArray, op, expectedValueAsByteArray);

        } else if (BsonHelper.isBsonDocument(expectedValue) && BsonHelper.isBsonDocument(actualValue)) {
            truthnessOfComparison = compareDocumentValues(actualValue, op, expectedValue);
        } else {
            // If both types are supported, but no actual comparison logic is defined,
            // we considered them to be incompatible, therefore the comparison returns true
            // only if the comparison operator is NOT_EQUALS_TO. Otherwise returns false.
            truthnessOfComparison = op == NOT_EQUALS_TO ? TRUE_C : C_FALSE;
        }

        /**
         * Taint Analysis
         */
        if (BsonHelper.isObjectId(actualValue) || BsonHelper.isObjectId(expectedValue)) {
            String expectedValueAsString = expectedValue.toString();
            String actualValueAsString = actualValue.toString();
            if (taintHandler != null && op == EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(expectedValueAsString, actualValueAsString, false);
            }
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

    private Truthness compareBinaryData(byte[] actualValueAsByteArray,
                                        ComparisonOperatorType op,
                                        byte[] expectedValueAsByteArray) {

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

    private static Truthness compareNumberValues(Number actualValueAsNumber, ComparisonOperatorType op, Number expectedValueAsNumber) {
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

    /**
     * Compares two nullable values using the specified comparison operator.
     *
     * @param actualValue the actual value to compare, which can be null
     * @param op the comparison operator to use
     * @param expectedValue the expected value to compare against, which can be null
     * @return a Truthness object representing the result of the comparison
     */
    public Truthness compareNullableValues(Object actualValue, ComparisonOperatorType op, Object expectedValue) {
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
    public Truthness computeHeuristicContainsElement(Object element, List<?> list) {
        Objects.requireNonNull(list);

        if (list.isEmpty()) {
            return C_FALSE;
        } else {
            Truthness res = buildOrAggregationTruthness(list.stream()
                    .map(expectedValue -> {
                        if (BsonHelper.isBsonRegularExpression(expectedValue)) {
                            String expectedPattern = BsonHelper.bsonRegexGetPattern(expectedValue);
                            String expectedOptions = BsonHelper.bsonRegexGetOptions(expectedValue);
                            Pattern pattern = Pattern.compile(expectedPattern);
                            return evaluateRegularExpression(element.toString(), pattern, taintHandler);
                        } else {
                            return compareNullableValues(element, EQUALS_TO, expectedValue);
                        }
                    })
                    .toArray(Truthness[]::new));

            return buildSafeScaledTruthness(res);
        }
    }

    /**
     * Computes a heuristic truthness score for the "$in" operation, evaluating whether the actual value
     * is contained within the expected value list. If the actual value is a list, each element is compared
     * against the expected value list, and the results are aggregated.
     *
     * @param actualValue the actual value to compare, which can be a single value or a list of values
     * @param expectedValueList the list of expected values to compare against
     * @return a Truthness object representing the result of the "in" operation evaluation
     */
    public Truthness computeHeuristicInOperation(Object actualValue, List<?> expectedValueList) {
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

    /**
     * Evaluates the modulus operation on the actual value and compares it to the expected remainder.
     * The method checks if the actual value is a number, computes the remainder of the division
     * by the specified expectedDivisor, and then compares it to the expected remainder. The result is
     * returned as a Truthness object, which captures both the definitive match result and the closeness to a potential match.
     *
     * @param actualValue the actual value to evaluate, which must be a Number
     * @param expectedDivisor the divisor to use for the modulus operation
     * @param expectedRemainder the expected remainder to compare against
     * @return a Truthness object representing the result of the evaluation
     */
    public Truthness evaluateMod(Object actualValue, long expectedDivisor, long expectedRemainder) {
        if (!(actualValue instanceof Number)) {
            return C_FALSE;
        }

        final long actualValueAsLong = ((Number) actualValue).longValue();
        final long actualRemainder = actualValueAsLong % expectedDivisor;
        Truthness res = getEqualityTruthness(actualRemainder, expectedRemainder);
        return buildSafeScaledTruthness(res);
    }


    /**
     * Evaluates the "$bitsAllClear" operation, checking if all bits specified by the bitmask are clear
     * in the actual value.
     *
     * @param actualValue the actual value to evaluate, which must be a Number
     * @param bitmask the bitmask specifying which bits to check
     * @return a Truthness object representing the result of the evaluation
     */
    public Truthness evaluateBitsAllClearOperation(Object actualValue, long bitmask) {
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

    /**
     * Evaluates the "$bitsAllSet" operation, checking if all bits specified by the bitmask are set in the actual value.
     *
     * @param actualValue the actual value to evaluate, which must be a Number
     * @param bitmask the bitmask specifying which bits to check
     * @return a Truthness object representing the result of the evaluation
     */
    public Truthness evaluateBitsAllSetOperation(Object actualValue, long bitmask) {

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

    /**
     * Evaluates the "$bitsAnyClear" operation, checking if any bits specified by the bitmask are clear in the actual value.
     *
     * @param actualValue the actual value to evaluate, which must be a Number
     * @param bitmask the bitmask specifying which bits to check
     * @return a Truthness object representing the result of the evaluation
     */
    public Truthness evaluateBitsAnyClearOperation(Object actualValue, long bitmask) {

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

    /**
     * Evaluates the "$bitsAnySet" operation, checking if any bits specified by the bitmask are set in the actual value.
     *
     * @param actualValue the actual value to evaluate, which must be a Number
     * @param bitmask the bitmask specifying which bits to check
     * @return a Truthness object representing the result of the evaluation
     */
    public Truthness evaluateBitsAnySetOperation(Object actualValue, long bitmask) {

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

    /**
     * Evaluates the distance between two geographical points and checks if it falls within a specified range.
     * The method calculates the distance between the point represented by the given longitude and latitude
     * and the point represented by the actualValue, which is expected to be a GeoJSON Point.
     * The distance is then compared against the provided minimum and maximum distance thresholds.
     * The result is returned as a Truthness object, which captures both the definitive match result and
     * the closeness to a potential match.
     *
     * @param actualValue the actual value to evaluate, which must be a GeoJSON Point to represent a geographical location
     * @param minDistance the minimum distance threshold
     * @param maxDistance the maximum distance threshold
     * @param longitude the longitude of the reference point
     * @param latitude the latitude of the reference point
     * @param geoSpatialModel the model to use for calculating the distance
     * @return a Truthness object representing the result of the evaluation
     */
    public static Truthness evaluateDistanceBetweenPoints(Object actualValue, double minDistance,
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

    /**
     * Evaluates the equality of two values, considering the possibility that either value may be a list.
     * If the top-level comparison indicates equality, it returns that result. Otherwise, it attempts
     * to unwrap the actual value if it is a list and compare each element against the expected
     * value, aggregating the results to determine if any element matches.
     *
     * @param actualValue the actual value to compare, which can be a single value or a list of values
     * @param expectedValue the expected value to compare against
     * @return a Truthness object representing the result of the equality evaluation
     */
    public Truthness evaluateEquality(Object actualValue, Object expectedValue) {
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
    public Truthness evaluateWithArrayUnwrapping(Object actualValue,
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

    /**
     * Computes the heuristic score for a {@code $geoIntersects} query. The document's actual
     * value must itself be a supported GeoJSON geometry document; otherwise the condition is
     * considered not satisfied. When it is, the heuristic is based on the approximate planar
     * distance between the two geometries (see {@link GeoJsonGeometryIntersection}): 0 (true)
     * when they share at least one point, and a scaled falseness proportional to how far apart
     * they are otherwise.
     */
    static Truthness evaluateGeoIntersects(GeoJsonGeometry queryGeometry, Object actualValue) {
        Objects.requireNonNull(queryGeometry);

        if (!isBsonDocument(actualValue)) {
            return C_FALSE;
        }

        final GeoJsonGeometry actualGeometry;
        try {
            actualGeometry = GeoJsonUtils.toGeoJsonGeometry(actualValue);
        } catch (IllegalArgumentException e) {
            return C_FALSE;
        }

        double distance = GeoJsonGeometryIntersection.distance(queryGeometry, actualGeometry);
        return getEqualityTruthness(distance, 0.0);
    }

    /**
     * Computes the heuristic score for a {@code $geoWithin} query. The document's actual value
     * must itself be a supported GeoJSON geometry document; otherwise the condition is considered
     * not satisfied. When it is, the heuristic is based on the approximate planar distance of the
     * farthest-outside point of the document's geometry from the area(s) enclosed by
     * {@code areaGeometry} (see {@link GeoJsonGeometryIntersection#distanceToContainment}): 0 (true)
     * when the document's geometry lies entirely within (or on the boundary of) that area, and a
     * scaled falseness proportional to how far outside it otherwise.
     */
    static Truthness evaluateGeoWithin(GeoJsonGeometry areaGeometry, Object actualValue) {
        Objects.requireNonNull(areaGeometry);

        if (!isBsonDocument(actualValue)) {
            return C_FALSE;
        }

        final GeoJsonGeometry actualGeometry;
        try {
            actualGeometry = GeoJsonUtils.toGeoJsonGeometry(actualValue);
        } catch (IllegalArgumentException e) {
            return C_FALSE;
        }

        double distance = GeoJsonGeometryIntersection.distanceToContainment(actualGeometry, areaGeometry);
        return getEqualityTruthness(distance, 0.0);
    }
}
