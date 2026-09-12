package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import static org.evomaster.client.java.controller.mongo.MongoHeuristicsCalculatorHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.*;
import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.SPHERICAL;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * The MongoHeuristicsCalculator class provides methods to compute heuristic scores for MongoDB-like
 * query operations against a set of documents. These heuristics aim to measure how closely a
 * document satisfies the given query conditions. The class supports a variety of query operators,
 * including equality, inequality, comparison, and logical operations.
 *
 */
public class MongoHeuristicsCalculator {


    /**
     * A handler responsible for managing taint propagation and tracking
     * during the execution of heuristic calculations. This object is used
     * to process and handle specific taint-related operations, such as
     * string equality comparisons and regular expression evaluations.
     */
    private final TaintHandler taintHandler;

    /**
     * A helper component for handling the internal heuristic computation logic
     * within the context of Mongo operations. It provides utilities and lower-level
     * methods that are invoked by the main computation flow in
     * {@code MongoHeuristicsCalculator}.
     */
    private final MongoHeuristicsCalculatorHelper helper;

    public MongoHeuristicsCalculator() {
        this(null);
    }

    public MongoHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
        this.helper = new MongoHeuristicsCalculatorHelper(taintHandler);
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
        } else if (operation instanceof BitsAllClearOperation) {
            return evaluate((BitsAllClearOperation) operation, actualValue);
        } else if (operation instanceof BitsAllSetOperation) {
            return evaluate((BitsAllSetOperation) operation, actualValue);
        } else if (operation instanceof BitsAnyClearOperation) {
            return evaluate((BitsAnyClearOperation) operation, actualValue);
        } else if (operation instanceof BitsAnySetOperation) {
            return evaluate((BitsAnySetOperation) operation, actualValue);
        } else if (operation instanceof NotOperation) {
            return evaluate((NotOperation) operation, actualValue);
        } else if (operation instanceof RegexOperation) {
            return evaluate((RegexOperation) operation, actualValue);
        } else if (operation instanceof NearSphereOperation) {
            return evaluate((NearSphereOperation) operation, actualValue);
        } else if (operation instanceof NearOperation) {
            return evaluate((NearOperation) operation, actualValue);
        } else if (operation instanceof ElemMatchOperation) {
            return evaluate((ElemMatchOperation) operation, actualValue);
        } else if (operation instanceof GeoIntersectsOperation) {
            return evaluate((GeoIntersectsOperation) operation, actualValue);
        } else if (operation instanceof GeoWithinOperation) {
            return evaluate((GeoWithinOperation) operation, actualValue);
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

        final double minDistance = operation.hasMinDistance() ? operation.getMinDistance() : 0.0;
        final double maxDistance = operation.hasMaxDistance() ? operation.getMaxDistance() : Double.MAX_VALUE;

        return helper.evaluateDistanceBetweenPoints(
                minDistance, maxDistance,
                longitude, latitude,
                actualValue,
                model);
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
            return helper.computeHeuristicContainsElement(expectedValue, (List<?>) actualValue);
        } else {
            return helper.compareNullableValues(
                    expectedValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO, actualValue
            );
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
            return helper.computeHeuristicContainsElement(expectedValue, (List<?>) actualValue).invert();
        } else {
            return helper.compareNullableValues(
                    expectedValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO, actualValue
            );
        }
    }


    private Truthness evaluate(GreaterThanOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        Object expectedValue = operation.getValue();
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN, value
                ));

    }

    private Truthness evaluate(GreaterThanEqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);
        final Object expectedValue = operation.getValue();

        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN_EQUALS, value
                ));

    }

    private Truthness evaluate(LessThanOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);
        final Object expectedValue = operation.getValue();

        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN, value
                ));
    }

    private Truthness evaluate(LessThanEqualsOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        final Object expectedValue = operation.getValue();
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.compareNullableValues(
                        expectedValue,
                        SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN_EQUALS, value
                ));
    }

    /**
     * Evaluates a nested structure or a single value using a provided heuristic function.
     * For nested structures that are lists, it aggregates the results of applying the heuristic
     * function to each element in the list. For single values, it directly applies the heuristic.
     *
     * @param value            the value to evaluate, which can either be a single value or a list of values
     * @param elementHeuristic a function to compute the heuristic for each element or the single value
     * @return a Truthness object representing the heuristic of the evaluated input
     */
    private Truthness evaluateWithArrayUnwrapping(Object value,
                                                  Function<Object, Truthness> elementHeuristic) {
        Objects.requireNonNull(elementHeuristic);

        if (!(value instanceof List<?>)) {
            // value is not an array. Inspect only this value.
            return elementHeuristic.apply(value);
        } else {
            // value is an array. Inspect all elements.
            List<?> values = (List<?>) value;
            if (values.isEmpty()) {
                return C_FALSE;
            }
            Truthness orAggregation = buildOrAggregationTruthness(values.stream()
                    .map(elementHeuristic)
                    .toArray(Truthness[]::new));
            return buildSafeScaledTruthness(orAggregation);
        }
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
        final Truthness res = helper.computeHeuristicInOperation(actualValue, expectedValueList);
        return res;
    }

    private Truthness evaluate(NotInOperation<?> operation, Object actualValue) {
        Objects.requireNonNull(operation);

        List<?> expectedValueList = operation.getValues();
        final Truthness res = helper.computeHeuristicInOperation(actualValue, expectedValueList);
        return res.invert();
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
                return helper.compareNullableValues(
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
                .map(expectedValue -> helper.computeHeuristicContainsElement(expectedValue, actualValues))
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
                        .map(listElement -> evaluateOnArrayElement(
                                operation.getCondition(), listElement))
                        .toArray(Truthness[]::new));
                return buildSafeScaledTruthness(orAggregation);
            }
        }
    }

    private Truthness evaluateOnArrayElement(QueryOperation condition, Object element) {
        if (condition instanceof AndOperation) {
            return buildAndAggregationTruthness(((AndOperation) condition).getConditions().stream()
                    .map(child -> evaluateOnArrayElement(child, element))
                    .toArray(Truthness[]::new));
        } else if (condition instanceof OrOperation) {
            return buildOrAggregationTruthness(((OrOperation) condition).getConditions().stream()
                    .map(child -> evaluateOnArrayElement(child, element))
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
                            helper.compareNonNullValues(actualFieldName,
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
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.evaluateMod(divisor, expectedRemainder, value));
    }

    private Truthness evaluate(BitsAllClearOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.evaluateBitsAllClearOperation(operation.getBitmask(), value));
    }

    private Truthness evaluate(BitsAnyClearOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.evaluateBitsAnyClearOperation(operation.getBitmask(), value));
    }

    private Truthness evaluate(BitsAllSetOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.evaluateBitsAllSetOperation(operation.getBitmask(), value));
    }

    private Truthness evaluate(BitsAnySetOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return evaluateWithArrayUnwrapping(actualValue,
                value -> helper.evaluateBitsAnySetOperation(operation.getBitmask(), value));

    }

    private Truthness evaluate(NotOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);

        QueryOperation condition = operation.getCondition();
        Truthness conditionTruthness = evaluateOnArrayElement(condition, actualValue);
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

        final double maxDistance = operation.hasMaxDistance() ? operation.getMaxDistance() : Double.MAX_VALUE;
        final double minDistance = operation.hasMinDistance() ? operation.getMinDistance() : 0.0;

        return helper.evaluateDistanceBetweenPoints(
                minDistance, maxDistance,
                longitude, latitude,
                actualValue,
                SPHERICAL);
    }

    private Truthness evaluate(GeoIntersectsOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return helper.evaluateGeoIntersects(operation.getGeometry(), actualValue);
    }

    private Truthness evaluate(GeoWithinOperation operation, Object actualValue) {
        Objects.requireNonNull(operation);
        return helper.evaluateGeoWithin(operation.getGeometry(), actualValue);
    }

}
