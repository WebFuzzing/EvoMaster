package org.evomaster.client.java.controller.internal.db.dynamodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeType;
import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.DynamoDbComparisonType;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BeginsWithOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BetweenOperation;
import org.evomaster.client.java.controller.dynamodb.operations.ContainsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.ExistsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.InOperation;
import org.evomaster.client.java.controller.dynamodb.operations.NotOperation;
import org.evomaster.client.java.controller.dynamodb.operations.OrOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.SizeOperation;
import org.evomaster.client.java.controller.dynamodb.operations.TypeOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.ComparisonOperation;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

/**
 * Computes DynamoDB predicate heuristics directly as {@link Truthness} values.
 *
 * <p>
 * Evaluating one item returns predicate Truthness, while evaluating multiple source items
 * keeps the best non-matching {@code ofTrue} value when no item satisfies the query.
 */
public class DynamoDbHeuristicsCalculator {

    private final TaintHandler taintHandler;

    /**
     * Creates a calculator without taint tracking callbacks.
     */
    public DynamoDbHeuristicsCalculator() {
        this(null);
    }

    /**
     * Creates a calculator with an optional taint callback used during string
     * comparisons.
     * @param taintHandler optional callback used when strings are compared
     */
    public DynamoDbHeuristicsCalculator(TaintHandler taintHandler) {
        this.taintHandler = taintHandler;
    }

    /**
     * Computes the distance to satisfying a DynamoDB query.
     * @param keyCondition parsed key condition, or {@code null}
     * @param filterExpression parsed filter expression, or {@code null}
     * @param items source items to evaluate
     * @return distance to the query's true branch
     */
    public double computeDistance(QueryOperation keyCondition, QueryOperation filterExpression,
            Collection<Map<String, Object>> items) {
        return (double) 1.0F - this.computeQuery(keyCondition, filterExpression, items).getOfTrue();
    }

    /**
     * Computes query truthness while preserving DynamoDB's key-before-filter evaluation
     * order.
     * @param keyCondition parsed key condition, or {@code null}
     * @param filterExpression parsed filter expression, or {@code null}
     * @param items source items to evaluate
     * @return query-level truthness
     */
    public Truthness computeQuery(QueryOperation keyCondition, QueryOperation filterExpression,
            Collection<Map<String, Object>> items) {
        if (items != null && !items.isEmpty()) {
            Truthness keyTruthness = keyCondition == null ? TruthnessUtils.TRUE_TRUTHNESS
                    : this.computeCondition(keyCondition, items);
            if (filterExpression == null) {
                return keyTruthness;
            } else {
                List<Map<String, Object>> filterCandidates = new ArrayList<>();

                for (Map<String, Object> item : items) {
                    if (keyCondition == null || this.computeExpression(keyCondition, item).isTrue()) {
                        filterCandidates.add(item);
                    }
                }

                Truthness filterTruthness = this.computeCondition(filterExpression, filterCandidates);
                return TruthnessUtils.buildAndAggregationTruthness(keyTruthness, filterTruthness);
            }
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    /**
     * Computes query-level truthness over all available source items.
     * @param operation parsed predicate
     * @param items source items to evaluate
     * @return query-level truthness
     */
    public Truthness computeCondition(QueryOperation operation, Collection<Map<String, Object>> items) {
        if (operation != null && items != null && !items.isEmpty()) {
            double maxOfTrue = 0.0d;

            for (Map<String, Object> item : items) {
                Truthness expressionTruthness = this.computeExpression(operation, item);
                if (expressionTruthness.isTrue()) {
                    return TruthnessUtils.TRUE_TRUTHNESS;
                }

                maxOfTrue = Math.max(maxOfTrue, expressionTruthness.getOfTrue());
            }

            return TruthnessUtils.buildScaledTruthness(0.1, maxOfTrue);
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    /**
     * Computes the truthness of one parsed DynamoDB predicate over one concrete item.
     * @param operation parsed predicate
     * @param item concrete item represented as plain Java values
     * @return truthness for the predicate on the item
     */
    public Truthness computeExpression(QueryOperation operation, Map<String, Object> item) {
        return operation != null && item != null ? this.calculateTruthness(operation, item)
                : TruthnessUtils.FALSE_TRUTHNESS;
    }

    private Truthness calculateTruthness(QueryOperation operation, Map<String, Object> item) {
        if (operation instanceof ComparisonOperation) {
            ComparisonOperation<?> comparison = (ComparisonOperation<?>) operation;
            return this.truthnessForComparison(comparison, item, DynamoDbComparisonType.fromOperation(comparison));
        } else if (operation instanceof BetweenOperation) {
            return this.truthnessForBetween((BetweenOperation) operation, item);
        } else if (operation instanceof InOperation) {
            return this.truthnessForIn((InOperation<?>) operation, item);
        } else if (operation instanceof AndOperation) {
            return this.truthnessForAnd((AndOperation) operation, item);
        } else if (operation instanceof OrOperation) {
            return this.truthnessForOr((OrOperation) operation, item);
        } else if (operation instanceof NotOperation) {
            return this.calculateTruthness(((NotOperation) operation).getCondition(), item).invert();
        } else if (operation instanceof ExistsOperation) {
            return this.truthnessForExists((ExistsOperation) operation, item);
        } else if (operation instanceof TypeOperation) {
            return this.truthnessForType((TypeOperation) operation, item);
        } else if (operation instanceof BeginsWithOperation) {
            return this.truthnessForBeginsWith((BeginsWithOperation) operation, item);
        } else if (operation instanceof ContainsOperation) {
            return this.truthnessForContains((ContainsOperation) operation, item);
        } else if (operation instanceof SizeOperation) {
            return this.truthnessForSize((SizeOperation) operation, item);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }

    private Truthness truthnessForComparison(ComparisonOperation<?> operation, Map<String, Object> item,
            DynamoDbComparisonType comparisonType) {
        DynamoDbAttributeValueHelper.ValueLookup lookup = DynamoDbAttributeValueHelper.lookupByPath(item,
                operation.getFieldName());
        if (!lookup.found) {
            return comparisonType == DynamoDbComparisonType.NOT_EQUALS ? TruthnessUtils.TRUE_TRUTHNESS
                    : TruthnessUtils.FALSE_TRUTHNESS;
        } else {
            return this.comparisonTruthness(lookup.value, operation.getValue(), comparisonType);
        }
    }

    private Truthness truthnessForBetween(BetweenOperation operation, Map<String, Object> item) {
        return this.evaluateExistingField(operation.getFieldName(), item, (actual) -> {
            Truthness lower = this.comparisonTruthness(actual, operation.getLowerBound(),
                    DynamoDbComparisonType.GREATER_THAN_EQUALS);
            Truthness upper = this.comparisonTruthness(actual, operation.getUpperBound(),
                    DynamoDbComparisonType.LESS_THAN_EQUALS);
            return TruthnessUtils.buildAndAggregationTruthness(lower, upper);
        });
    }

    private Truthness truthnessForIn(InOperation<?> operation, Map<String, Object> item) {
        return this.evaluateExistingField(operation.getFieldName(), item,
                (actual) -> this.inTruthness(actual, operation.getValues()));
    }

    private Truthness truthnessForAnd(AndOperation operation, Map<String, Object> item) {
        List<QueryOperation> conditions = operation.getConditions();
        if (conditions != null && !conditions.isEmpty()) {
            List<Truthness> truthnesses = this.calculateTruthnessList(item, conditions);
            return TruthnessUtils.buildAndAggregationTruthness(truthnesses.toArray(new Truthness[0]));
        } else {
            return TruthnessUtils.TRUE_TRUTHNESS;
        }
    }

    private Truthness truthnessForOr(OrOperation operation, Map<String, Object> item) {
        List<QueryOperation> conditions = operation.getConditions();
        if (conditions != null && !conditions.isEmpty()) {
            List<Truthness> truthnesses = this.calculateTruthnessList(item, conditions);
            return TruthnessUtils.buildOrAggregationTruthness(truthnesses.toArray(new Truthness[0]));
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    private List<Truthness> calculateTruthnessList(Map<String, Object> item, List<QueryOperation> conditions) {
        List<Truthness> truthnesses = new ArrayList<>(conditions.size());

        for (QueryOperation condition : conditions) {
            truthnesses.add(this.calculateTruthness(condition, item));
        }

        return truthnesses;
    }

    private Truthness truthnessForExists(ExistsOperation operation, Map<String, Object> item) {
        DynamoDbAttributeValueHelper.ValueLookup lookup = DynamoDbAttributeValueHelper.lookupByPath(item,
                operation.getFieldName());
        Truthness existsTruthness = lookup.found ? TruthnessUtils.TRUE_TRUTHNESS
                : this.missingPathTruthness(operation.getFieldName(), item);
        return operation.isExists() ? existsTruthness : existsTruthness.invert();
    }

    private Truthness truthnessForType(TypeOperation operation, Map<String, Object> item) {
        DynamoDbAttributeType expectedType = operation.getExpectedType();
        return expectedType == null ? TruthnessUtils.FALSE_TRUTHNESS
                : this.evaluateExistingField(operation.getFieldName(), item, (actual) -> {
                    DynamoDbAttributeType actualType = DynamoDbAttributeValueHelper.resolveAttributeType(actual);
                    if (actualType == expectedType) {
                        return TruthnessUtils.TRUE_TRUTHNESS;
                    } else {
                        return actualType.isScalarSetVariantOf(expectedType) ? TruthnessUtils.FALSE_TRUTHNESS_BETTER
                                : TruthnessUtils.FALSE_TRUTHNESS;
                    }
                });
    }

    private Truthness truthnessForBeginsWith(BeginsWithOperation operation, Map<String, Object> item) {
        return this.evaluateExistingField(operation.getFieldName(), item, (value) -> {
            if (value instanceof String && operation.getPrefix() != null) {
                String actual = (String) value;
                String prefix = String.valueOf(operation.getPrefix());
                if (actual.startsWith(prefix)) {
                    return TruthnessUtils.TRUE_TRUTHNESS;
                } else {
                    String candidate = actual.length() < prefix.length() ? actual
                            : actual.substring(0, prefix.length());
                    return TruthnessUtils.getStringEqualityTruthness(candidate, prefix);
                }
            } else {
                return TruthnessUtils.FALSE_TRUTHNESS;
            }
        });
    }

    private Truthness truthnessForContains(ContainsOperation operation, Map<String, Object> item) {
        return this.evaluateExistingField(operation.getFieldName(), item, (actual) -> {
            Object expected = operation.getExpectedValue();
            if (actual instanceof String && expected != null) {
                return this.stringContainsTruthness((String) actual, String.valueOf(expected));
            } else {
                return actual instanceof Collection
                        ? this.collectionMembershipTruthness(expected, (Collection<?>) actual)
                        : this.equalityTruthness(actual, expected);
            }
        });
    }

    private Truthness truthnessForSize(SizeOperation operation, Map<String, Object> item) {
        return this.evaluateExistingField(operation.getFieldName(), item, (actual) -> {
            Integer actualSize = this.computeSize(actual);
            return operation.getComparator() == null ? TruthnessUtils.FALSE_TRUTHNESS
                    : this.comparisonTruthness(actualSize, operation.getExpectedValue(), operation.getComparator());
        });
    }

    private Truthness comparisonTruthness(Object actual, Object expected, DynamoDbComparisonType comparisonType) {
        if (comparisonType == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        } else if (actual == null && expected == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        } else if (actual != null && expected != null) {
            Truthness unscaled = this.unscaledComparisonTruthness(actual, expected, comparisonType);
            return unscaled.isTrue() ? unscaled
                    : TruthnessUtils.buildScaledTruthness(0.15000000000000002, unscaled.getOfTrue());
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS_BETTER;
        }
    }

    private Truthness unscaledComparisonTruthness(Object actual, Object expected,
            DynamoDbComparisonType comparisonType) {
        if (actual instanceof String && expected instanceof String) {
            return this.unscaledStringComparisonTruthness((String) actual, (String) expected, comparisonType);
        } else {
            switch (comparisonType) {
                case EQUALS:
                    return this.unscaledEqualityTruthness(actual, expected);
                case NOT_EQUALS:
                    return this.unscaledEqualityTruthness(actual, expected).invert();
                case GREATER_THAN:
                case GREATER_THAN_EQUALS:
                case LESS_THAN:
                case LESS_THAN_EQUALS:
                    return this.unscaledOrderingTruthness(actual, expected, comparisonType);
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator: " + comparisonType);
            }
        }
    }

    private Truthness unscaledStringComparisonTruthness(String actual, String expected,
            DynamoDbComparisonType comparisonType) {
        switch (comparisonType) {
            case EQUALS:
                if (this.taintHandler != null) {
                    this.taintHandler.handleTaintForStringEquals(actual, expected, false);
                }

                return TruthnessUtils.getStringEqualityTruthness(actual, expected);
            case NOT_EQUALS:
                return TruthnessUtils.getStringEqualityTruthness(actual, expected).invert();
            case GREATER_THAN:
                return actual.compareTo(expected) > 0 ? TruthnessUtils.TRUE_TRUTHNESS : TruthnessUtils.FALSE_TRUTHNESS;
            case GREATER_THAN_EQUALS:
                return actual.compareTo(expected) >= 0 ? TruthnessUtils.TRUE_TRUTHNESS : TruthnessUtils.FALSE_TRUTHNESS;
            case LESS_THAN:
                return actual.compareTo(expected) < 0 ? TruthnessUtils.TRUE_TRUTHNESS : TruthnessUtils.FALSE_TRUTHNESS;
            case LESS_THAN_EQUALS:
                return actual.compareTo(expected) <= 0 ? TruthnessUtils.TRUE_TRUTHNESS : TruthnessUtils.FALSE_TRUTHNESS;
            default:
                throw new IllegalArgumentException("Unsupported comparison operator: " + comparisonType);
        }
    }

    private Truthness equalityTruthness(Object actual, Object expected) {
        return this.comparisonTruthness(actual, expected, DynamoDbComparisonType.EQUALS);
    }

    private Truthness unscaledEqualityTruthness(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double left = ((Number) actual).doubleValue();
            double right = ((Number) expected).doubleValue();
            return Double.isFinite(left) && Double.isFinite(right) ? TruthnessUtils.getEqualityTruthness(left, right)
                    : TruthnessUtils.FALSE_TRUTHNESS;
        } else if (actual instanceof Boolean && expected instanceof Boolean) {
            return TruthnessUtils.getEqualityTruthness((Boolean) actual, (Boolean) expected);
        } else if (actual instanceof byte[] && expected instanceof byte[]) {
            return TruthnessUtils.getEqualityTruthness((byte[]) actual, (byte[]) expected);
        } else if (actual instanceof UUID && expected instanceof UUID) {
            return TruthnessUtils.getEqualityTruthness((UUID) actual, (UUID) expected);
        } else {
            return actual.equals(expected) ? TruthnessUtils.TRUE_TRUTHNESS : TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    private Truthness unscaledOrderingTruthness(Object actual, Object expected, DynamoDbComparisonType comparisonType) {
        Double difference = this.orderingDifference(actual, expected);
        if (difference != null && Double.isFinite(difference)) {
            switch (comparisonType) {
                case GREATER_THAN:
                    return TruthnessUtils.getLessThanTruthness(0.0d, difference);
                case GREATER_THAN_EQUALS:
                    return TruthnessUtils.getLessThanTruthness(difference, 0.0d).invert();
                case LESS_THAN:
                    return TruthnessUtils.getLessThanTruthness(difference, 0.0d);
                case LESS_THAN_EQUALS:
                    return TruthnessUtils.getLessThanTruthness(0.0d, difference).invert();
                default:
                    return TruthnessUtils.FALSE_TRUTHNESS;
            }
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    private Truthness evaluateExistingField(String fieldName, Map<String, Object> item,
            Function<Object, Truthness> evaluator) {
        DynamoDbAttributeValueHelper.ValueLookup lookup = DynamoDbAttributeValueHelper.lookupByPath(item, fieldName);
        return !lookup.found ? TruthnessUtils.FALSE_TRUTHNESS : evaluator.apply(lookup.value);
    }

    private Truthness missingPathTruthness(String requestedPath, Map<String, Object> item) {
        if (requestedPath == null) {
            return TruthnessUtils.FALSE_TRUTHNESS;
        } else {
            Set<String> paths = DynamoDbAttributeValueHelper.documentPaths(item);
            if (paths.isEmpty()) {
                return TruthnessUtils.FALSE_TRUTHNESS;
            } else {
                Truthness bestMatch = TruthnessUtils.FALSE_TRUTHNESS;

                for (String path : paths) {
                    Truthness candidate = TruthnessUtils.getStringEqualityTruthness(path, requestedPath);
                    if (candidate.getOfTrue() > bestMatch.getOfTrue()) {
                        bestMatch = candidate;
                    }
                }

                return bestMatch;
            }
        }
    }

    private Truthness collectionMembershipTruthness(Object actual, Collection<?> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            List<Truthness> truthnesses = new ArrayList<>();
            if (actual instanceof Collection) {
                for (Object element : (Collection<?>) actual) {
                    for (Object candidate : candidates) {
                        truthnesses.add(this.equalityTruthness(element, candidate));
                    }
                }
            } else {
                for (Object candidate : candidates) {
                    truthnesses.add(this.equalityTruthness(actual, candidate));
                }
            }

            return truthnesses.isEmpty() ? TruthnessUtils.FALSE_TRUTHNESS
                    : TruthnessUtils.buildOrAggregationTruthness(truthnesses.toArray(new Truthness[0]));
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    private Truthness inTruthness(Object actual, Collection<?> candidates) {
        if (candidates != null && !candidates.isEmpty()) {
            double maxOfTrue = 0.0d;

            for (Object actualValue : actual instanceof Collection ? (Collection<?>) actual
                    : Collections.singletonList(actual)) {
                for (Object candidate : candidates) {
                    Truthness truthness = this.equalityTruthness(actualValue, candidate);
                    if (truthness.isTrue()) {
                        return TruthnessUtils.TRUE_TRUTHNESS;
                    }

                    maxOfTrue = Math.max(maxOfTrue, truthness.getOfTrue());
                }
            }

            return TruthnessUtils.buildScaledTruthness(0.1, maxOfTrue);
        } else {
            return TruthnessUtils.FALSE_TRUTHNESS;
        }
    }

    private Truthness stringContainsTruthness(String source, String expected) {
        if (source.contains(expected)) {
            return TruthnessUtils.TRUE_TRUTHNESS;
        } else {
            int windowSize = Math.min(source.length(), expected.length());
            Truthness bestTruthness = TruthnessUtils.getStringEqualityTruthness(source.substring(0, windowSize),
                    expected);

            for (int i = 1; i + windowSize <= source.length(); ++i) {
                String candidate = source.substring(i, i + windowSize);
                Truthness candidateTruthness = TruthnessUtils.getStringEqualityTruthness(candidate, expected);
                if (candidateTruthness.getOfTrue() > bestTruthness.getOfTrue()) {
                    bestTruthness = candidateTruthness;
                }
            }

            return bestTruthness;
        }
    }

    private Double orderingDifference(Object left, Object right) {
        if (left != null && right != null) {
            if (left instanceof Number && right instanceof Number) {
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();
            }
            if (left instanceof Comparable<?> && right instanceof Comparable<?>
                    && left.getClass().equals(right.getClass())) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) left;
                return (double) comparable.compareTo(right);
            }
        }
        return null;
    }

    private Integer computeSize(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return ((String) value).length();
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        } else {
            return value instanceof byte[] ? ((byte[]) value).length : null;
        }
    }

}
