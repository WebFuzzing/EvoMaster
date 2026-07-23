package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.internal.db.mongo.MongoDistanceWithMetrics;
import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.utils.BsonHelper;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.heuristic.SqlExpressionEvaluator;
import org.evomaster.client.java.sql.internal.TaintHandler;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.heuristic.ConversionHelper.convertToInstant;

import java.util.*;
import java.util.stream.StreamSupport;

public class MongoHeuristicsCalculator {

    public static final double C = 0.1;
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
        } else if (operation instanceof ElemMatchOperation) {
            return computeHeuristic((ElemMatchOperation) operation, document);
        } else if (operation instanceof ModOperation) {
            return computeHeuristic((ModOperation) operation, document);
        } else if (operation instanceof NotOperation) {
            return computeHeuristic((NotOperation) operation, document);
        } else if (operation instanceof TypeOperation) {
            return computeHeuristic((TypeOperation) operation, document);
        } else if (operation instanceof NearSphereOperation) {
            return computeHeuristic((NearSphereOperation) operation, document);
        } else if (operation instanceof TrueOperation) {
            return computeHeuristic((TrueOperation) operation, document);
        } else {
            throw new IllegalArgumentException("Unsupported QueryOperation type: " + operation.getClass().getName());
        }
    }

    /**
     * This one-line implementation is kept for consistency with the other computeHeuristic methods,
     * even though it always returns TRUE_C.
     *
     * @param operation
     * @param document
     * @return
     */
    private Truthness computeHeuristic(TrueOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);
        return TRUE_C;
    }

    private static Truthness computeHeuristicComparisonNonNullValues(Object actualValue, Object expectedValue, SqlExpressionEvaluator.ComparisonOperatorType comparisonOperatorType) {
        Objects.requireNonNull(actualValue);
        Objects.requireNonNull(expectedValue);

        final Truthness truthnessOfComparison;
        if (actualValue instanceof Number && expectedValue instanceof Number) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForNumberComparison((Number) actualValue, (Number) expectedValue, comparisonOperatorType);
        } else if (actualValue instanceof String && expectedValue instanceof String) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForStringComparison((String) actualValue, (String) expectedValue, comparisonOperatorType);
        } else if (actualValue instanceof Boolean && expectedValue instanceof Boolean) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForBooleanComparison((Boolean) actualValue, (Boolean) expectedValue, comparisonOperatorType);
        }  else if (BsonHelper.isObjectId(actualValue) || BsonHelper.isObjectId(expectedValue)) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForStringComparison(actualValue.toString(), expectedValue.toString(), comparisonOperatorType);
        } else if (actualValue instanceof Date || expectedValue instanceof Date) {
            truthnessOfComparison = SqlExpressionEvaluator.calculateTruthnessForInstantComparison(convertToInstant(actualValue), convertToInstant(expectedValue), comparisonOperatorType);
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + actualValue.getClass().getName());
        }
        return truthnessOfComparison;
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

        return computeHeuristicComparisonNullableValues(
                expectedValue,
                actualValue,
                SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO);
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
        return computeHeuristicComparisonNullableValues(
                expectedValue,
                actualValue,
                SqlExpressionEvaluator.ComparisonOperatorType.NOT_EQUALS_TO);
    }


    private Truthness computeHeuristic(GreaterThanOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        Object expectedValue = operation.getValue();
        String fieldName = operation.getFieldName();

        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN);

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
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.GREATER_THAN_EQUALS);

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
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN);
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
            return computeHeuristicComparisonNullableValues(
                    expectedValue,
                    actualValue,
                    SqlExpressionEvaluator.ComparisonOperatorType.MINOR_THAN_EQUALS);

        }
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

        final Truthness res;
        if (actualValue instanceof List<?>) {
            List<?> actualValueList = (List<?>) actualValue;
            res = buildOrAggregationTruthness(actualValueList.stream()
                    .map(value -> computeHeuristic(value, expectedValueList))
                    .toArray(Truthness[]::new));
        } else {
            res = computeHeuristic(actualValue, expectedValueList);
        }
        return res;
    }

    private Truthness computeHeuristic(Object actualValue, List<?> expectedValueList) {
        Objects.requireNonNull(expectedValueList);

        Truthness res = buildOrAggregationTruthness(expectedValueList.stream()
                .map(expectedValue -> computeHeuristicComparisonNullableValues(expectedValue, actualValue, SqlExpressionEvaluator.ComparisonOperatorType.EQUALS_TO))
                .toArray(Truthness[]::new));
        return res;
    }

    private Truthness computeHeuristic(NotInOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        List<?> expectedValues = operation.getValues();
        final String fieldName = operation.getFieldName();

        if (!documentContainsField(document, fieldName)) {
            return TRUE_C;
        } else {
            Object actualValue = getValue(document, fieldName);
            return computeHeuristic(actualValue, expectedValues).invert();
        }
    }

    private Truthness computeHeuristic(AllOperation<?> operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        List<?> expectedValues = operation.getValues();
        final String fieldName = operation.getFieldName();
        if (!documentContainsField(document, fieldName)) {
            return C_FALSE;
        } else if (expectedValues.isEmpty()) {
            return C_FALSE;
        } else {
            Object actualValues = getValue(document, fieldName);
            if (actualValues == null || !(actualValues instanceof List<?>)) {
                return C_FALSE;
            } else {
                List<?> actualValuesList = (List<?>) actualValues;
                if (actualValuesList.isEmpty()) {
                    return C_FALSE;
                } else {
                    Truthness res = buildAndAggregationTruthness(actualValuesList
                            .stream()
                            .map(actualValuesListElement ->
                                    computeHeuristic(actualValuesListElement, expectedValues))
                            .toArray(Truthness[]::new));
                    return buildSafeScaledTruthness(res);
                }
            }
        }
    }

    private static Truthness buildSafeScaledTruthness(Truthness truthness) {
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
        throw new IllegalArgumentException("Not implemented yet");
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

        if (actualValue == null || !(actualValue instanceof Number)) {
            return C_FALSE;
        } else {
            long actualRemainder = ((Number) actualValue).longValue() % divisor;
            Truthness res = getEqualityTruthness(actualRemainder, expectedRemainder);
            return buildSafeScaledTruthness(res);
        }
    }


    private Truthness computeHeuristic(NotOperation operation, Object document) {
        requireNonNullQueryAndDocument(operation, document);

        String fieldName = operation.getFieldName();
        if (!documentContainsField(document, fieldName)) {
            return TRUE_C;
        } else {
            QueryOperation condition = operation.getCondition();
            return computeHeuristicOnDocument(condition, document).invert();
        }
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
        throw new IllegalArgumentException("Not implemented yet");
    }

}
