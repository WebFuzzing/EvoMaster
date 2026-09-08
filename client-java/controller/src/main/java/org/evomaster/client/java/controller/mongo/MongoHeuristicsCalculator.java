package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;


import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class MongoHeuristicsCalculator {

    // TODO these constants should be replaced by DistanceHelper constants
    public static final double C = 0.1;
    // TODO These constants should be refactored by TruthnessUtils constants
    public static final Truthness TRUE_C = new Truthness(1.0, C);
    public static final Truthness C_FALSE = new Truthness(C, 1.0);


    private final TaintHandler taintHandler;

    public MongoHeuristicsCalculator() {
        this(null);
    }

    public MongoHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }


    public MongoDistanceWithMetrics computeDistanceDocuments(Object query, Iterable<?> documents) {
        long count = StreamSupport.stream(documents.spliterator(), false).count();
        Truthness heuristicScoreCollection = getTruthnessToEmpty((int) count).invert();

        QueryOperation queryOperation = parseQuery(query);
        Truthness hCondition = computeHeuristicOnDocuments(queryOperation, documents);

        Truthness hQuery = buildAndAggregationTruthness(heuristicScoreCollection, hCondition);

        // Map truthness to distance where 0 is true.
        // If it's true, distance 0.
        // If it's false, distance is 1.0 - ofTrue.
        double distance = hQuery.isTrue() ? 0.0 : 1.0 - hQuery.getOfTrue();

        return new MongoDistanceWithMetrics(distance, (int) count);
    }

    private Truthness computeHeuristicOnDocuments(QueryOperation operation, Iterable<?> documents) {
        long count = StreamSupport.stream(documents.spliterator(), false).count();
        if (count == 0) {
            return C_FALSE;
        }

        double maxOfTrue = 0;
        boolean first = true;
        for (Object doc : documents) {
            double ofTrue = computeHeuristicOnDocument(operation, doc).getOfTrue();
            if (first || ofTrue > maxOfTrue) {
                maxOfTrue = ofTrue;
            }
            first = false;
        }

        return buildSafeScaledTruthness(maxOfTrue);
    }

    private static Truthness buildSafeScaledTruthness(double maxOfTrue) {
        if (maxOfTrue == 1.0) {
            return TRUE_C;
        } else {
            return buildScaledTruthness(C, maxOfTrue);
        }
    }

    /**
     * Compute a "branch" distance heuristics.
     *
     * @param query    the QUERY clause that we want to resolve as true
     * @param document a document in the database for which we want to calculate the distance
     * @return a branch distance, where 0 means that the document would make the QUERY resolve as true
     */
    Truthness computeHeuristicDocument(Object query, Object document) {
        QueryOperation operation = parseQuery(query);
        return computeHeuristicOnDocument(operation, document);
    }

    private QueryOperation parseQuery(Object query) {
        return new QueryParser().parse(query);
    }

    private Truthness computeHeuristicOnDocument(QueryOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        if (operation instanceof AndOperation) {
            return computeHeuristic((AndOperation) operation, document);
        } else if (operation instanceof OrOperation) {
            return computeHeuristic((OrOperation) operation, document);
        } else if (operation instanceof NorOperation) {
            return computeHeuristic((NorOperation) operation, document);
        } else if (operation instanceof ExistsOperation) {
            return computeHeuristic((ExistsOperation) operation, document);
        } else if (operation instanceof EqualsOperation<?>) {
            return computeHeuristic((EqualsOperation<?>) operation, document);
        } else if (operation instanceof NotEqualsOperation<?>) {
            return computeHeuristic((NotEqualsOperation<?>) operation, document);
        } else if (operation instanceof GreaterThanOperation<?>) {
            return computeHeuristic((GreaterThanOperation<?>) operation, document);
        } else if (operation instanceof GreaterThanEqualsOperation<?>) {
            return computeHeuristic((GreaterThanEqualsOperation<?>) operation, document);
        } else if (operation instanceof LessThanOperation<?>) {
            return computeHeuristic((LessThanOperation<?>) operation, document);
        } else if (operation instanceof LessThanEqualsOperation<?>) {
            return computeHeuristic((LessThanEqualsOperation<?>) operation, document);
        } else if (operation instanceof InOperation<?>) {
            return computeHeuristic((InOperation<?>) operation, document);
        } else if (operation instanceof NotInOperation<?>) {
            return computeHeuristic((NotInOperation<?>) operation, document);
        } else if (operation instanceof AllOperation<?>) {
            return computeHeuristic((AllOperation<?>) operation, document);
        } else if (operation instanceof SizeOperation) {
            return computeHeuristic((SizeOperation) operation, document);
        } else if (operation instanceof ModOperation) {
            return computeHeuristic((ModOperation) operation, document);
        } else if (operation instanceof BitsOperation) {
            return computeHeuristic((BitsOperation) operation, document);
        } else if (operation instanceof NotOperation) {
            return computeHeuristic((NotOperation) operation, document);
        } else if (operation instanceof TypeOperation) {
            return computeHeuristic((TypeOperation) operation, document);
        } else if (operation instanceof RegexOperation) {
            return computeHeuristic((RegexOperation) operation, document);
        } else if (operation instanceof NearSphereOperation) {
            return computeHeuristic((NearSphereOperation) operation, document);
        } else if (operation instanceof NearOperation) {
            return computeHeuristic((NearOperation) operation, document);
        } else if (operation instanceof ElemMatchOperation) {
            return computeHeuristic((ElemMatchOperation) operation, document);
        } else if (operation instanceof EmptyOperation) {
            return computeHeuristic((EmptyOperation) operation, document);
        } else {
            throw new IllegalArgumentException("Unsupported QueryOperation type: " + operation.getClass().getName());
        }
    }

    private Truthness computeHeuristic(RegexOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        Objects.requireNonNull(operation.getPattern());
        Objects.requireNonNull(operation.getOptions());

        final String fieldName = operation.getFieldName();
        final Pattern pattern = operation.getPattern();

        final Object actualValue;
        if (documentContainsField(document, fieldName)) {
            actualValue = getValue(document, fieldName);
        } else {
            actualValue = null;
        }

        if (actualValue == null) {
            return C_FALSE;
        } else if (actualValue instanceof String) {
            final String inputValue = (String) actualValue;
            return computeHeuristicRegularExpression(inputValue, pattern);
        } else if (actualValue instanceof List<?>) {
            List<?> actualValueList = (List<?>) actualValue;
            if (actualValueList.isEmpty()) {
                return C_FALSE;
            } else {
                Truthness[] results = actualValueList.stream()
                        .filter(element -> element instanceof String)
                        .map(element -> (String) element)
                        .map(element -> computeHeuristicRegularExpression(element, pattern))
                        .toArray(Truthness[]::new);
                return buildOrAggregationTruthness(results);
            }
        } else {
            return C_FALSE;
        }
    }

    private Truthness computeHeuristicRegularExpression(String inputValue, Pattern pattern) {
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

    private Truthness computeHeuristic(NearOperation operation, Object document) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(document);
        Objects.requireNonNull(operation.getFieldName());

        final String fieldName = operation.getFieldName();
        final Object fieldValue = getValue(document, fieldName);

        final double longitude = operation.getLongitude();
        final double latitude = operation.getLatitude();

        GeoSpatialModel model = operation.hasLegacyCoordinates()
                ? GeoSpatialModel.PLANAR
                : SPHERICAL;
        return computeHeuristic(operation, longitude, latitude, fieldValue, model);
    }

    /**
     * This one-line implementation is kept for consistency with the other computeHeuristic methods,
     * even though it always returns TRUE_C.
     *
     * @param operation
     * @param document
     * @return
     */
    private Truthness computeHeuristic(EmptyOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        return TRUE_C;
    }

    private Truthness computeHeuristicComparisonNonNullValues(Object actualValue, Object expectedValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(actualValue);
        Objects.requireNonNull(expectedValue);

        final Truthness truthnessOfComparison;
        if (actualValue instanceof Number && expectedValue instanceof Number) {
            final Number actualNumber = (Number) actualValue;
            final Number expectedNumber = (Number) expectedValue;
            truthnessOfComparison = computeHeuristicComparisonNumberValues(actualNumber, expectedNumber, comparisonOperatorType);

        } else if (actualValue instanceof String && expectedValue instanceof String) {
            String actualString = (String) actualValue;
            String expectedString = (String) expectedValue;
            if (taintHandler != null && comparisonOperatorType == SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO) {
                taintHandler.handleTaintForStringEquals(actualString, expectedString, false);
            }
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForStringComparison(actualString, expectedString, comparisonOperatorType);

        } else if (actualValue instanceof Boolean && expectedValue instanceof Boolean) {
            int actualIntValue = toIntValue((Boolean) actualValue);
            int expectedIntValue = toIntValue((Boolean) expectedValue);
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison(
                    actualIntValue, expectedIntValue, comparisonOperatorType);

        } else if (actualValue instanceof List<?> && expectedValue instanceof List<?>) {
            truthnessOfComparison = calculateTruthnessForListComparison((List<?>) actualValue, (List<?>) expectedValue, comparisonOperatorType);

        } else if (actualValue instanceof Date && expectedValue instanceof Date) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForInstantComparison(convertToInstant(actualValue), convertToInstant(expectedValue), comparisonOperatorType);


        } else if (BsonHelper.isBsonTimestamp(actualValue) && BsonHelper.isBsonTimestamp(expectedValue)) {
            long actualTimestamp = BsonHelper.getBsonTimestampValue(actualValue);
            long expectedTimestamp = BsonHelper.getBsonTimestampValue(expectedValue);
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison(actualTimestamp, expectedTimestamp, comparisonOperatorType);

        } else if (BsonHelper.isObjectId(actualValue) || BsonHelper.isObjectId(expectedValue)) {
            String actualString = actualValue.toString();
            String expectedString = expectedValue.toString();
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

    private static Truthness computeHeuristicComparisonNumberValues(Number leftNumber, Number rightNumber, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType) {
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

    private static int toIntValue(Boolean actualValue) {
        return actualValue ? 1 : 0;
    }

    /**
     * Checks if the provided value is of a supported type for comparison.
     *
     * @param value
     * @return
     */
    private static boolean isTypeSupportedForComparison(Object value) {
        Objects.requireNonNull(value);

        return value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean ||
                value instanceof Date ||
                value instanceof List<?> ||
                BsonHelper.isObjectId(value) ||
                BsonHelper.isBsonTimestamp(value);
    }

    /**
     * Computes the heuristic score for a {"f",{"$eq": value }} query.
     * If the field "f" is not present, and the expected value is null, the condition is satisfied.
     * If the field "f" is not present, but the expected value is not null, the condition is not satisfied.
     * If the field "f" is present, null values are considered equal, and non-null values are compared
     * using the corresponding heuristic score for non-null values.
     *
     * @param operation the  {"f",{"$eq": value }} query
     * @param document  the BSON document to evaluate the heuristic score against
     * @return
     */
    private Truthness computeHeuristic(EqualsOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        String fieldName = operation.getFieldName();
        Object expectedValue = operation.getValue();

        Object actualValue;
        if (documentContainsField(document, fieldName)) {
            actualValue = getValue(document, fieldName);
        } else {
            actualValue = null;
        }

        if ((actualValue instanceof List<?>) && !(expectedValue instanceof List<?>)) {
            return computeHeuristicContainsElement(expectedValue, (List<?>) actualValue);
        } else {
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO);
        }
    }

    private Truthness computeHeuristicComparisonNullableValues(Object expectedValue, Object actualValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType) {
        if (expectedValue == null || actualValue == null) {
            switch (comparisonOperatorType) {
                case EQUALS_TO:
                    return (expectedValue == null && actualValue == null) ? TRUE_C : C_FALSE;
                case NOT_EQUALS_TO:
                    return (expectedValue == null && actualValue == null) ? C_FALSE : TRUE_C;
                case GREATER_THAN:
                case GREATER_THAN_EQUALS:
                case MINOR_THAN:
                case MINOR_THAN_EQUALS:
                    return C_FALSE;
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator type: " + comparisonOperatorType);
            }
        } else {
            Truthness valTruthness = computeHeuristicComparisonNonNullValues(actualValue,
                    expectedValue,
                    comparisonOperatorType);
            return buildSafeScaledTruthness(valTruthness);
        }
    }


    /**
     * Computes the heuristic score for a {"f",{"$ne": value}} query.
     * Evaluates whether the value of the specified field in a document is not equal
     * to the expected value. If the condition is satisfied, the score is inverted to
     * reflect the distance from the condition being false.
     *
     * @param operation the {"f",{"$ne": value}} query encapsulated as a NotEqualsOperation.
     *                  This operation specifies the field name and the expected value
     *                  for the inequality check.
     * @param document  the BSON document to evaluate the heuristic score against.
     *                  The document may or may not contain the field to be checked.
     * @return a Truthness object representing the distance of the document from meeting
     * the inequality condition, where one of the values (true or false) is 1,
     * and the other represents the distance to the alternate condition.
     */
    private Truthness computeHeuristic(NotEqualsOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Object expectedValue = operation.getValue();
        String fieldName = operation.getFieldName();

        Object actualValue;
        if (documentContainsField(document, fieldName)) {
            actualValue = getValue(document, fieldName);
        } else {
            actualValue = null;
        }
        if ((actualValue instanceof List<?>) && !(expectedValue instanceof List<?>)) {
            return computeHeuristicContainsElement(expectedValue, (List<?>) actualValue).invert();
        } else {
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO);
        }
    }


    private Truthness computeHeuristic(GreaterThanOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Object expectedValue = operation.getValue();
        String fieldName = operation.getFieldName();

        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristicForActualValueOrAnyElement(actualValue,
                    value -> computeHeuristicComparisonNullableValues(
                            expectedValue,
                            value,
                            SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN));

        }
    }

    private Truthness computeHeuristic(GreaterThanEqualsOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        final String fieldName = operation.getFieldName();
        final Object expectedValue = operation.getValue();

        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristicForActualValueOrAnyElement(actualValue,
                    value -> computeHeuristicComparisonNullableValues(
                            expectedValue,
                            value,
                            SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN_EQUALS));

        }
    }

    private Truthness computeHeuristic(LessThanOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        final String fieldName = operation.getFieldName();
        final Object expectedValue = operation.getValue();

        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristicForActualValueOrAnyElement(actualValue,
                    value -> computeHeuristicComparisonNullableValues(
                            expectedValue,
                            value,
                            SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN));
        }
    }

    private Truthness computeHeuristic(LessThanEqualsOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        final String fieldName = operation.getFieldName();
        final Object expectedValue = operation.getValue();

        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristicForActualValueOrAnyElement(actualValue,
                    value -> computeHeuristicComparisonNullableValues(
                            expectedValue,
                            value,
                            SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN_EQUALS));

        }
    }

    private Truthness computeHeuristicForActualValueOrAnyElement(Object actualValue,
                                                                  Function<Object, Truthness> elementHeuristic) {
        Objects.requireNonNull(elementHeuristic);

        if (!(actualValue instanceof List<?>)) {
            return elementHeuristic.apply(actualValue);
        }

        List<?> values = (List<?>) actualValue;
        if (values.isEmpty()) {
            return C_FALSE;
        }

        Truthness orAggregation = buildOrAggregationTruthness(values.stream()
                .map(elementHeuristic)
                .toArray(Truthness[]::new));
        return buildSafeScaledTruthness(orAggregation);
    }

    private Truthness computeHeuristic(OrOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Truthness[] results = operation.getConditions().stream()
                .map(condition -> computeHeuristicOnDocument(condition, document))
                .toArray(Truthness[]::new);
        return buildOrAggregationTruthness(results);
    }

    private Truthness computeHeuristic(AndOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Truthness[] results = operation.getConditions().stream()
                .map(condition -> computeHeuristicOnDocument(condition, document))
                .toArray(Truthness[]::new);
        return buildAndAggregationTruthness(results);
    }


    private Truthness computeHeuristic(InOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        List<?> expectedValueList = operation.getValues();
        final String fieldName = operation.getFieldName();

        final Object actualValue;
        if (documentContainsField(document, fieldName)) {
            actualValue = getValue(document, fieldName);
        } else {
            // If the document does not have a field
            // with that name, we consider the field
            // value to be null
            actualValue = null;
        }

        final Truthness res = computeHeuristicInOperation(actualValue, expectedValueList);
        return res;
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
    private Truthness computeHeuristicContainsElement(Object element, List<?> list) {
        Objects.requireNonNull(list);

        if (list.isEmpty()) {
            return C_FALSE;
        } else {
            Truthness res = buildOrAggregationTruthness(list.stream()
                    .map(expectedValue -> computeHeuristicComparisonNullableValues(expectedValue,
                            element,
                            SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO))
                    .toArray(Truthness[]::new));
            return buildSafeScaledTruthness(res);
        }
    }

    private Truthness computeHeuristic(NotInOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        List<?> expectedValueList = operation.getValues();
        final String fieldName = operation.getFieldName();

        final Object actualValue;
        if (!documentContainsField(document, fieldName)) {
            actualValue = null;
        } else {
            actualValue = getValue(document, fieldName);
        }

        final Truthness res = computeHeuristicInOperation(actualValue, expectedValueList);
        return res.invert();
    }

    private Truthness computeHeuristicInOperation(Object actualValue, List<?> expectedValueList) {
        final Truthness res;
        if (actualValue instanceof List<?>) {
            List<?> actualValueList = (List<?>) actualValue;
            // first we try to match the actualValueList as a whole with any element of the expectedValueList
            Truthness[] arrayOfTruthnesses = expectedValueList.stream()
                    .filter(expectedValueListElement -> expectedValueListElement instanceof List<?>)
                    .map(expectedValueListElement -> (List<?>) expectedValueListElement)
                    .map(expectedValueInnerListElement ->
                            computeHeuristicListEquality(expectedValueInnerListElement, actualValueList))
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

    private Truthness computeHeuristic(AllOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        final List<?> expectedValues = operation.getValues();
        final String fieldName = operation.getFieldName();

        if (expectedValues.isEmpty()) {
            return C_FALSE;
        }

        if (!documentContainsField(document, fieldName)) {
            return isSingleNullExpectedValue(expectedValues) ? TRUE_C : C_FALSE;
        }

        final Object actualFieldValue = getValue(document, fieldName);
        if (actualFieldValue == null) {
            return isSingleNullExpectedValue(expectedValues) ? TRUE_C : C_FALSE;
        }

        if (!(actualFieldValue instanceof List<?>)) {
            if (expectedValues.size() != 1) {
                return C_FALSE;
            } else {
                return computeHeuristicComparisonNullableValues(
                        expectedValues.get(0),
                        actualFieldValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO);
            }
        }

        List<?> actualValues = (List<?>) actualFieldValue;
        if (actualValues.isEmpty()) {
            return C_FALSE;
        }

        Truthness res = buildAndAggregationTruthness(expectedValues.stream()
                .map(expectedValue -> computeHeuristicContainsElement(expectedValue, actualValues))
                .toArray(Truthness[]::new));
        return buildSafeScaledTruthness(res);
    }

    private static boolean isSingleNullExpectedValue(List<?> expectedValues) {
        return expectedValues.size() == 1 && expectedValues.get(0) == null;
    }

    private static Truthness buildSafeScaledTruthness(Truthness truthness) {
        Objects.requireNonNull(truthness);

        return buildSafeScaledTruthness(truthness.getOfTrue());
    }


    private Truthness computeHeuristic(SizeOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        Objects.requireNonNull(operation.getValue());

        if (!documentContainsField(document, operation.getFieldName())) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, operation.getFieldName());
            if (actualValue == null || !(actualValue instanceof List<?>)) {
                return C_FALSE;
            } else {
                int actualSize = ((List<?>) actualValue).size();
                int expectedSize = operation.getValue().intValue();
                Truthness res = getEqualityTruthness(actualSize, expectedSize);
                return buildSafeScaledTruthness(res);
            }
        }
    }


    private Truthness computeHeuristic(ElemMatchOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        Objects.requireNonNull(operation.getFieldName());
        Objects.requireNonNull(operation.getCondition());

        if (!documentContainsField(document, operation.getFieldName())) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, operation.getFieldName());
            if (actualValue == null || !(actualValue instanceof List<?>)) {
                return C_FALSE;
            } else {
                List<?> actualList = (List<?>) actualValue;
                if (actualList.isEmpty()) {
                    return C_FALSE;
                } else {
                    Truthness orAggregation = buildOrAggregationTruthness(actualList.stream()
                            .map(listElement -> computeHeuristicOnElemMatchElement(
                                    operation.getCondition(), listElement, document))
                            .toArray(Truthness[]::new));
                    return buildSafeScaledTruthness(orAggregation);
                }
            }
        }
    }

    private Truthness computeHeuristicOnElemMatchElement(QueryOperation condition, Object element, Object documentTemplate) {
        if (isBsonDocument(element)) {
            return computeHeuristicOnDocument(condition, element);
        }

        if (!(condition instanceof QueryOperationWithField)) {
            return C_FALSE;
        }

        Object elementDocument = newDocument(documentTemplate);
        appendToDocument(elementDocument, ((QueryOperationWithField) condition).getFieldName(), element);
        return computeHeuristicOnDocument(condition, elementDocument);
    }

    private Truthness computeHeuristic(ExistsOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        String expectedFieldName = operation.getFieldName();
        Set<String> actualFieldNames = documentKeys(document);
        final Truthness res;
        if (actualFieldNames.isEmpty()) {
            res = C_FALSE;
        } else {
            Truthness orTruthness = buildOrAggregationTruthness(actualFieldNames.stream()
                    .map(actualFieldName ->
                            computeHeuristicComparisonNonNullValues(actualFieldName,
                                    expectedFieldName,
                                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO))
                    .toArray(Truthness[]::new));
            res = buildSafeScaledTruthness(orTruthness);
        }

        if (operation.getBoolean() == true) {
            // "true" case of exists operation
            return res;
        } else {
            // "false" case of exists operation
            return res.invert();
        }
    }

    private static void requireNonNullQueryAndDocument(QueryOperation operation, Object document) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(document);
        if (!isBsonDocument(document)) {
            throw new IllegalArgumentException("The provided document is not a valid BSON document: " + document);
        }
    }

    private Truthness computeHeuristic(ModOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        Objects.requireNonNull(operation.getDivisor());
        Objects.requireNonNull(operation.getRemainder());

        long divisor = operation.getDivisor().longValue();
        long expectedRemainder = operation.getRemainder().longValue();

        final String fieldName = operation.getFieldName();
        final Object actualValue;
        if (!documentContainsField(document, fieldName)) {
            actualValue = null;
        } else {
            actualValue = getValue(document, fieldName);
        }

        return computeHeuristicForActualValueOrAnyElement(actualValue,
                value -> computeHeuristicModOnSingleValue(value, divisor, expectedRemainder));
    }

    private Truthness computeHeuristicModOnSingleValue(Object value, long divisor, long expectedRemainder) {
        if (!(value instanceof Number)) {
            return C_FALSE;
        }

        long actualRemainder = ((Number) value).longValue() % divisor;
        Truthness res = getEqualityTruthness(actualRemainder, expectedRemainder);
        return buildSafeScaledTruthness(res);
    }

    private Truthness computeHeuristic(BitsOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        String fieldName = operation.getFieldName();
        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        }

        Object actualValue = getValue(document, fieldName);
        return computeHeuristicForActualValueOrAnyElement(actualValue,
                value -> computeHeuristicBitsOnSingleValue(value, operation));
    }

    private Truthness computeHeuristicBitsOnSingleValue(Object value, BitsOperation operation) {
        if (!(value instanceof Number)) {
            return C_FALSE;
        }

        final OptionalLong integralValue = getIntegralLongValue((Number) value);
        if (!integralValue.isPresent()) {
            return C_FALSE;
        }

        final long bitmask = operation.getBitmask();
        final long maskedValue = integralValue.getAsLong() & bitmask;
        final int numberOfSetBitsInMaskedValue = Long.bitCount(maskedValue);
        final int numberOfBitsInMask = Long.bitCount(bitmask);
        if (operation instanceof BitsAllClearOperation) {
            Truthness equalityTruthness = getEqualityTruthness(numberOfSetBitsInMaskedValue, 0);
            return buildSafeScaledTruthness(equalityTruthness);
        } else if (operation instanceof BitsAllSetOperation) {
            Truthness equalityTruthness = getEqualityTruthness(numberOfSetBitsInMaskedValue, numberOfBitsInMask);
            return buildSafeScaledTruthness(equalityTruthness);
        } else if (operation instanceof BitsAnyClearOperation) {
            Truthness lessThanTruthness = getLessThanTruthness(numberOfSetBitsInMaskedValue, numberOfBitsInMask);
            return buildSafeScaledTruthness(lessThanTruthness);
        } else if (operation instanceof BitsAnySetOperation) {
            Truthness lessThanTruthness = getLessThanTruthness(0, numberOfSetBitsInMaskedValue);
            return buildSafeScaledTruthness(lessThanTruthness);
        } else {
            throw new IllegalArgumentException("Unsupported BitsOperation type: " + operation.getClass().getName());
        }
    }

    private Truthness computeHeuristic(NotOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        QueryOperation condition = operation.getCondition();
        return computeHeuristicOnDocument(condition, document).invert();
    }

    private Truthness computeHeuristic(NorOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Truthness orRes = buildOrAggregationTruthness(operation.getConditions()
                .stream()
                .map(condition -> computeHeuristicOnDocument(condition, document))
                .toArray(Truthness[]::new));
        return orRes.invert();
    }

    private Truthness computeHeuristic(TypeOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        String fieldName = operation.getFieldName();
        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            final Object bsonType = operation.getType();
            String expectedType = getType(bsonType);

            Object actualValue = getValue(document, fieldName);
            String actualType = actualValue == null ? "null" : actualValue.getClass().getTypeName();

            final Truthness equalityTruthness = SqlExpressionEvaluator.getEqualityTruthness(actualType, expectedType);
            return buildSafeScaledTruthness(equalityTruthness);
        }
    }

    private Truthness computeHeuristic(NearSphereOperation operation, Object document) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(document);
        Objects.requireNonNull(operation.getFieldName());

        final String fieldName = operation.getFieldName();
        final Object fieldValue = getValue(document, fieldName);

        final double longitude = operation.getLongitude();
        final double latitude = operation.getLatitude();

        return computeHeuristic(operation, longitude, latitude, fieldValue, SPHERICAL);
    }


    private static Truthness computeHeuristic(AbstractProximityOperation abstractProximityOperation,
                                              double longitude,
                                              double latitude,
                                              Object fieldValue,
                                              GeoSpatialModel geoSpatialModel) {

        Objects.requireNonNull(abstractProximityOperation);
        double x1 = geoSpatialModel == SPHERICAL ? Math.toRadians(longitude) : longitude;
        double y1 = geoSpatialModel == SPHERICAL ? Math.toRadians(latitude) : latitude;
        double x2;
        double y2;

    /*
      GeoJSON Point in document.
      type key is case-sensitive.
      (https://datatracker.ietf.org/doc/html/rfc7946#section-1.4)
     */
        if (isBsonDocument(fieldValue)
                && GeoJsonUtils.isGeoJsonPoint(fieldValue)) {
            GeoJsonPoint geoJsonPoint = GeoJsonUtils.toGeoJsonPoint(fieldValue);
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
        double max = abstractProximityOperation.hasMaxDistance()
                ? abstractProximityOperation.getMaxDistance()
                : Double.MAX_VALUE;

        double min = abstractProximityOperation.hasMinDistance()
                ? abstractProximityOperation.getMinDistance()
                : 0.0;

        if (min <= distanceBetweenPoints
                && distanceBetweenPoints <= max) {
            return TRUE_C;
        }

        return (distanceBetweenPoints > max)
                ? getEqualityTruthness(distanceBetweenPoints, max)
                : getEqualityTruthness(distanceBetweenPoints, min);
    }

    private Truthness calculateTruthnessForListComparison(List<?> actualList, List<?> expectedList, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(actualList);
        Objects.requireNonNull(expectedList);

        final Truthness truthness = computeHeuristicListEquality(actualList, expectedList);
        switch (comparisonOperatorType) {
            case EQUALS_TO:
                return truthness;
            case NOT_EQUALS_TO:
                return truthness.invert();
            default:
                throw new IllegalArgumentException("Unsupported binary operator: " + comparisonOperatorType);
        }
    }

    private Truthness computeHeuristicListEquality(List<?> actualList, List<?> expectedList) {

        if (actualList.size() != expectedList.size()) {
            return C_FALSE;
        }

        if (actualList.isEmpty() && expectedList.isEmpty()) {
            return TRUE_C;
        }

        Truthness[] arrayOfTruthnesses = new Truthness[actualList.size()];
        for (int i = 0; i < actualList.size(); i++) {
            arrayOfTruthnesses[i] = computeHeuristicComparisonNullableValues(
                    actualList.get(i),
                    expectedList.get(i),
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO);
        }
        Truthness unscaledTruthness = buildAndAggregationTruthness(arrayOfTruthnesses);
        final Truthness truthness = buildSafeScaledTruthness(unscaledTruthness);
        return truthness;
    }


}
