package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import static org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculatorHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class MongoHeuristicsCalculator {


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
            double ofTrue = computeHeuristicQueryOperation(operation, doc).getOfTrue();
            if (first || ofTrue > maxOfTrue) {
                maxOfTrue = ofTrue;
            }
            first = false;
        }

        return buildSafeScaledTruthness(maxOfTrue);
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
        return computeHeuristicQueryOperation(operation, document);
    }

    private QueryOperation parseQuery(Object query) {
        return new QueryParser().parse(query);
    }

    private Truthness computeHeuristicQueryOperation(QueryOperation operation, Object value) {
        Objects.requireNonNull(operation);

        if (operation instanceof AndOperation) {
            return computeHeuristic((AndOperation) operation, value);
        } else if (operation instanceof OrOperation) {
            return computeHeuristic((OrOperation) operation, value);
        } else if (operation instanceof NorOperation) {
            return computeHeuristic((NorOperation) operation, value);
        } else if (operation instanceof EmptyOperation) {
            return computeHeuristic((EmptyOperation) operation, value);
        } else if (operation instanceof QueryOperationWithField) {
            return computeHeuristic((QueryOperationWithField) operation, value);
        } else {
            throw new IllegalArgumentException("Unsupported QueryOperation type: " + operation.getClass().getName());
        }
    }

    private Truthness computeHeuristic(QueryOperationWithField operation, Object document) {
        Objects.requireNonNull(operation);

        if (operation instanceof ExistsOperation) {
            return computeHeuristic((ExistsOperation) operation, document);
        } else if (operation instanceof TypeOperation) {
            return computeHeuristic((TypeOperation) operation, document);
        } else if (operation instanceof NotOperation) {
            return computeHeuristicQueryOperation(((NotOperation) operation).getCondition(), document).invert();
        } else {
            final String fieldName = operation.getFieldName();
            final Object actualValue;
            if (!fieldName.equalsIgnoreCase("$")) {
                if (!isBsonDocument(document)) {
                    return C_FALSE; // cannot extract the field from a non-document value
                } else {
                    actualValue = documentContainsField(document, fieldName) ? getValue(document, fieldName) : null;
                }
            } else {
                actualValue = document;
            }
            return evaluate(operation, actualValue);
        }
    }

    private Truthness evaluate(QueryOperation operation, Object actualValue) {
        if (operation instanceof EqualsOperation<?>) {
            return evaluate((EqualsOperation<?>) operation, actualValue);
        } else if (operation instanceof NotEqualsOperation<?>) {
            return evaluate((NotEqualsOperation<?>) operation, actualValue);
        } else if (operation instanceof GreaterThanOperation<?>) {
            return evaluate((GreaterThanOperation<?>) operation, actualValue);
        } else if (operation instanceof GreaterThanEqualsOperation<?>) {
            return evaluate((GreaterThanEqualsOperation<?>) operation, actualValue);
        } else if (operation instanceof LessThanOperation<?>) {
            return evaluate((LessThanOperation<?>) operation, actualValue);
        } else if (operation instanceof LessThanEqualsOperation<?>) {
            return evaluate((LessThanEqualsOperation<?>) operation, actualValue);
        } else if (operation instanceof InOperation<?>) {
            return evaluate((InOperation<?>) operation, actualValue);
        } else if (operation instanceof NotInOperation<?>) {
            return evaluate((NotInOperation<?>) operation, actualValue);
        } else if (operation instanceof AllOperation<?>) {
            return evaluate((AllOperation<?>) operation, actualValue);
        } else if (operation instanceof SizeOperation) {
            return evaluate((SizeOperation) operation, actualValue);
        } else if (operation instanceof ModOperation) {
            return evaluate((ModOperation) operation, actualValue);
        } else if (operation instanceof BitsOperation) {
            return evaluate((BitsOperation) operation, actualValue);
        } else if (operation instanceof NotOperation) {
            return computeHeuristicOnValue((NotOperation) operation, actualValue);
        } else if (operation instanceof RegexOperation) {
            return evaluate((RegexOperation) operation, actualValue);
        } else if (operation instanceof NearSphereOperation) {
            return evaluate((NearSphereOperation) operation, actualValue);
        } else if (operation instanceof NearOperation) {
            return evaluate((NearOperation) operation, actualValue);
        } else if (operation instanceof ElemMatchOperation) {
            return evaluate((ElemMatchOperation) operation, actualValue);
        } else {
            throw new IllegalArgumentException("Unsupported QueryOperation type: " + operation.getClass().getName());
        }

    }

    private Truthness evaluate(RegexOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final Pattern pattern = operation.getPattern();
        if (actualValue == null) {
            return C_FALSE;
        } else if (actualValue instanceof String) {
            final String inputValue = (String) actualValue;
            return evaluateRegularExpression(inputValue, pattern, taintHandler);
        } else if (actualValue instanceof List<?>) {
            List<?> actualValueList = (List<?>) actualValue;
            if (actualValueList.isEmpty()) {
                return C_FALSE;
            } else {
                Truthness[] results = actualValueList.stream()
                        .filter(element -> element instanceof String)
                        .map(element -> (String) element)
                        .map(element -> evaluateRegularExpression(element, pattern, taintHandler))
                        .toArray(Truthness[]::new);
                return buildOrAggregationTruthness(results);
            }
        } else {
            return C_FALSE;
        }
    }

    private Truthness evaluate(NearOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final double longitude = operation.getLongitude();
        final double latitude = operation.getLatitude();

        GeoSpatialModel model = operation.hasLegacyCoordinates()
                ? GeoSpatialModel.PLANAR
                : SPHERICAL;
        return evaluate(operation, longitude, latitude, actualValue, model);
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

    private Truthness compareNonNullValues(Object leftValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, Object rightValue) {
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

    private static Truthness compareNumberValues(Number leftNumber, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, Number rightNumber) {
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
     * @param operation   the  {"f",{"$eq": value }} query
     * @param actualValue the value to evaluate the heuristic score against
     * @return
     */
    private Truthness evaluate(EqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);
        Object expectedValue = operation.getValue();
        if ((actualValue instanceof List<?>) && !(expectedValue instanceof List<?>)) {
            return computeHeuristicContainsElement(expectedValue, (List<?>) actualValue);
        } else {
            return compareNullableValues(
                    expectedValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, actualValue
            );
        }
    }

    private Truthness compareNullableValues(Object leftValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, Object rightValue) {
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
     * Computes the heuristic score for a {"f",{"$ne": value}} query.
     * Evaluates whether the value of the specified field in a document is not equal
     * to the expected value. If the condition is satisfied, the score is inverted to
     * reflect the distance from the condition being false.
     *
     * @param operation   the {"f",{"$ne": value}} query encapsulated as a NotEqualsOperation.
     *                    This operation specifies the field name and the expected value
     *                    for the inequality check.
     * @param actualValue the actual value in the document being evaluated. This value is
     *                    obtained from the document and is compared against the expected value
     *                    specified in the operation.
     * @return a Truthness object representing the distance of the document from meeting
     * the inequality condition, where one of the values (true or false) is 1,
     * and the other represents the distance to the alternate condition.
     */
    private Truthness evaluate(NotEqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        Object expectedValue = operation.getValue();
        if ((actualValue instanceof List<?>) && !(expectedValue instanceof List<?>)) {
            return computeHeuristicContainsElement(expectedValue, (List<?>) actualValue).invert();
        } else {
            return compareNullableValues(
                    expectedValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO, actualValue
            );
        }
    }


    private Truthness evaluate(GreaterThanOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        Object expectedValue = operation.getValue();
        return evaluateNested(actualValue,
                value -> compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN, value
                ));

    }

    private Truthness evaluate(GreaterThanEqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);
        final Object expectedValue = operation.getValue();

        return evaluateNested(actualValue,
                value -> compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN_EQUALS, value
                ));

    }

    private Truthness evaluate(LessThanOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);
        final Object expectedValue = operation.getValue();

        return evaluateNested(actualValue,
                value -> compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN, value
                ));
    }

    private Truthness evaluate(LessThanEqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final Object expectedValue = operation.getValue();
        return evaluateNested(actualValue,
                value -> compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN_EQUALS, value
                ));
    }

    /**
     * Evaluates a nested structure or a single value using a provided heuristic function.
     * For nested structures that are lists, it aggregates the results of applying the heuristic
     * function to each element in the list. For single values, it directly applies the heuristic.
     *
     * @param value the value to evaluate, which can either be a single value or a list of values
     * @param elementHeuristic a function to compute the heuristic for each element or the single value
     * @return a Truthness object representing the heuristic of the evaluated input
     */
    private Truthness evaluateNested(Object value,
                                     Function<Object, Truthness> elementHeuristic) {
        Objects.requireNonNull(elementHeuristic);

        if (!(value instanceof List<?>)) {
            return elementHeuristic.apply(value);
        }

        List<?> values = (List<?>) value;
        if (values.isEmpty()) {
            return C_FALSE;
        }

        Truthness orAggregation = buildOrAggregationTruthness(values.stream()
                .map(elementHeuristic)
                .toArray(Truthness[]::new));
        return buildSafeScaledTruthness(orAggregation);
    }

    private Truthness computeHeuristic(OrOperation operation, Object document) {
        Objects.requireNonNull(operation);

        Truthness[] results = operation.getConditions().stream()
                .map(condition -> computeHeuristicQueryOperation(condition, document))
                .toArray(Truthness[]::new);
        return buildOrAggregationTruthness(results);
    }

    private Truthness computeHeuristic(AndOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Truthness[] results = operation.getConditions().stream()
                .map(condition -> computeHeuristicQueryOperation(condition, document))
                .toArray(Truthness[]::new);
        return buildAndAggregationTruthness(results);
    }


    private Truthness evaluate(InOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        List<?> expectedValueList = operation.getValues();
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
                    .map(expectedValue -> compareNullableValues(expectedValue,
                            SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, element
                    ))
                    .toArray(Truthness[]::new));
            return buildSafeScaledTruthness(res);
        }
    }

    private Truthness evaluate(NotInOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        List<?> expectedValueList = operation.getValues();
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

    private Truthness evaluate(AllOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final List<?> expectedValues = operation.getValues();

        if (expectedValues.isEmpty()) {
            return C_FALSE;
        }

        if (actualValue == null) {
            return (expectedValues.size() == 1 && expectedValues.get(0) == null)
                    ? TRUE_C
                    : C_FALSE;
        }

        if (!(actualValue instanceof List<?>)) {
            if (expectedValues.size() != 1) {
                return C_FALSE;
            } else {
                return compareNullableValues(
                        expectedValues.get(0),
                        SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, actualValue
                );
            }
        }

        List<?> actualValues = (List<?>) actualValue;
        if (actualValues.isEmpty()) {
            return C_FALSE;
        }

        Truthness res = buildAndAggregationTruthness(expectedValues.stream()
                .map(expectedValue -> computeHeuristicContainsElement(expectedValue, actualValues))
                .toArray(Truthness[]::new));
        return buildSafeScaledTruthness(res);
    }

    private Truthness evaluate(SizeOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        if (actualValue == null || !(actualValue instanceof List<?>)) {
            return C_FALSE;
        } else {
            int actualSize = ((List<?>) actualValue).size();
            int expectedSize = operation.getValue().intValue();
            Truthness res = getEqualityTruthness(actualSize, expectedSize);
            return buildSafeScaledTruthness(res);
        }
    }


    private Truthness evaluate(ElemMatchOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        if (actualValue == null || !(actualValue instanceof List<?>)) {
            return C_FALSE;
        } else {
            List<?> actualList = (List<?>) actualValue;
            if (actualList.isEmpty()) {
                return C_FALSE;
            } else {
                Truthness orAggregation = buildOrAggregationTruthness(actualList.stream()
                        .map(listElement -> computeHeuristicOnElement(
                                operation.getCondition(), listElement))
                        .toArray(Truthness[]::new));
                return buildSafeScaledTruthness(orAggregation);
            }
        }
    }

    private Truthness computeHeuristicOnElement(QueryOperation condition, Object element) {
        if (condition instanceof AndOperation) {
            return buildAndAggregationTruthness(((AndOperation) condition).getConditions().stream()
                    .map(child -> computeHeuristicOnElement(child, element))
                    .toArray(Truthness[]::new));
        } else if (condition instanceof OrOperation) {
            return buildOrAggregationTruthness(((OrOperation) condition).getConditions().stream()
                    .map(child -> computeHeuristicOnElement(child, element))
                    .toArray(Truthness[]::new));
        } else if (condition instanceof QueryOperationWithField
                && "$".equals(((QueryOperationWithField) condition).getFieldName())) {
            // The parser uses a synthetic field for operators applied to the element itself.
            return evaluate(condition, element);
        }

        if (isBsonDocument(element)) {
            return computeHeuristicQueryOperation(condition, element);
        }

        return C_FALSE;
    }

    private Truthness computeHeuristic(ExistsOperation operation, Object input) {
        Objects.requireNonNull(operation);

        if (!isBsonDocument(input)) {
            // If the input is not a BSON document, the existence of a field is always false.
            return C_FALSE;
        }

        String expectedFieldName = operation.getFieldName();
        Set<String> actualFieldNames = documentKeys(input);
        final Truthness res;
        if (actualFieldNames.isEmpty()) {
            res = C_FALSE;
        } else {
            Truthness orTruthness = buildOrAggregationTruthness(actualFieldNames.stream()
                    .map(actualFieldName ->
                            compareNonNullValues(actualFieldName,
                                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, expectedFieldName
                            ))
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

    private Truthness evaluate(ModOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        long divisor = operation.getDivisor().longValue();
        long expectedRemainder = operation.getRemainder().longValue();
        return evaluateNested(actualValue,
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

    private Truthness evaluate(BitsOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return evaluateNested(actualValue,
                value -> evaluateOnSingleValue(operation, value));
    }

    private Truthness evaluateOnSingleValue(BitsOperation operation,
                                            Object value) {
        Objects.requireNonNull(operation);

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

    private Truthness computeHeuristicOnValue(NotOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        QueryOperation condition = operation.getCondition();
        Truthness conditionTruthness = computeHeuristicOnElement(condition, actualValue);
        return conditionTruthness.invert();
    }

    private Truthness computeHeuristic(NorOperation operation, Object document) {
        Objects.requireNonNull(operation);

        Truthness orRes = buildOrAggregationTruthness(operation.getConditions()
                .stream()
                .map(condition -> computeHeuristicQueryOperation(condition, document))
                .toArray(Truthness[]::new));
        return orRes.invert();
    }

    private Truthness computeHeuristic(TypeOperation operation, Object document) {
        Objects.requireNonNull(operation);

        if (!isBsonDocument(document)) {
            return C_FALSE;
        }

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

    private Truthness evaluate(NearSphereOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final double longitude = operation.getLongitude();
        final double latitude = operation.getLatitude();

        return evaluate(operation, longitude, latitude, actualValue, SPHERICAL);
    }


    private static Truthness evaluate(AbstractProximityOperation abstractProximityOperation,
                                      double longitude,
                                      double latitude,
                                      Object actualValue,
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

    private Truthness compareNonNullLists(List<?> leftList, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType, List<?> rightList) {
        Objects.requireNonNull(leftList);
        Objects.requireNonNull(rightList);

        final Truthness truthness = computeHeuristicListEquality(leftList, rightList);
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
            arrayOfTruthnesses[i] = compareNullableValues(
                    actualList.get(i),
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, expectedList.get(i)
            );
        }
        Truthness unscaledTruthness = buildAndAggregationTruthness(arrayOfTruthnesses);
        final Truthness truthness = buildSafeScaledTruthness(unscaledTruthness);
        return truthness;
    }


}
