package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BeginsWithOperation;
import org.evomaster.client.java.controller.dynamodb.operations.BetweenOperation;
import org.evomaster.client.java.controller.dynamodb.operations.InOperation;
import org.evomaster.client.java.controller.dynamodb.operations.OrOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.GreaterThanOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.LessThanEqualsOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.LessThanOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Best-effort, "Neo4j style" seed collector for DynamoDB read predicates.
 * <p>
 * Walks key conditions first and filter expressions second in deterministic left-to-right order
 * across {@code AND} conjunctions, seeding flat scalar attributes from {@code =}, {@code <},
 * {@code <=}, {@code >}, {@code >=}, {@code BETWEEN}, {@code IN}, and {@code begins_with}
 * predicates using first-value-wins precedence (plus top-level {@code OR} unpacking for
 * {@code BatchGetItem}).
 * <p>
 * Unsupported constructs are skipped without aborting sibling conjuncts:
 * <ul>
 *   <li>{@code OR} inside filter expressions, {@code NOT}, and {@code <>}</li>
 *   <li>Functions without an unambiguous scalar seed: {@code contains},
 *       {@code attribute_exists} / {@code attribute_not_exists}, {@code size}, and
 *       {@code attribute_type}</li>
 *   <li>Nested document paths ({@code a.b}, {@code a[0]}) and non-scalar or non-finite values</li>
 * </ul>
 * <p>
 * Trade-off: supporting these directly requires a full-blown clause satisfiability checker
 * Skipping them keeps extraction single-pass and linear in expression size while still seeding
 * primary keys and boundary values for {@link DynamoDbHeuristicsCalculator} and gene mutation
 * to refine during search.
 */
public final class DynamoDbSeedCollector {

    /** Replacement non-empty seed used when a non-exact predicate seeds an empty string on a primary key. */
    private static final String NON_EMPTY_PRIMARY_KEY_FALLBACK = "a";

    /** Holds collected flat scalar attribute seeds and the names seeded by exact equality. */
    public static final class CollectedSeeds {

        private final Map<String, DynamoDbAttributeValueDto> attributes;
        private final Set<String> exactEqualityAttributes;

        private CollectedSeeds(Map<String, DynamoDbAttributeValueDto> attributes, Set<String> exactEqualityAttributes) {
            this.attributes = attributes;
            this.exactEqualityAttributes = exactEqualityAttributes;
        }

        /** @return mutable map of seeded attributes in deterministic insertion order */
        public Map<String, DynamoDbAttributeValueDto> getAttributes() {
            return attributes;
        }
    }

    /**
     * Collects best-effort scalar seeds from a query's key condition and filter expression.
     *
     * @param keyCondition parsed key condition, or {@code null}
     * @param filterExpression parsed filter expression, or {@code null}
     * @return collected seeds, or {@code null} when no flat scalar attribute could be seeded
     */
    public CollectedSeeds collect(QueryOperation keyCondition, QueryOperation filterExpression) {
        Map<String, DynamoDbAttributeValueDto> attributes = new LinkedHashMap<>();
        Set<String> exactEqualityAttributes = new LinkedHashSet<>();
        collectOperation(keyCondition, attributes, exactEqualityAttributes);
        collectOperation(filterExpression, attributes, exactEqualityAttributes);
        if (attributes.isEmpty()) {
            return null;
        }
        return new CollectedSeeds(attributes, exactEqualityAttributes);
    }

    /**
     * Unpacks top-level {@link OrOperation} branches for {@code BatchGetItem} in deterministic left-to-right order.
     *
     * @param keyCondition parsed {@code BatchGetItem} key condition, or {@code null}
     * @return list of per-key branch operations
     */
    public List<QueryOperation> unpackBatchGetBranches(QueryOperation keyCondition) {
        if (keyCondition == null) {
            return Collections.emptyList();
        }
        List<QueryOperation> branches = new ArrayList<>();
        List<QueryOperation> pending = new ArrayList<>();
        pending.add(keyCondition);
        while (!pending.isEmpty()) {
            QueryOperation current = pending.remove(pending.size() - 1);
            if (current instanceof OrOperation) {
                List<QueryOperation> conditions = ((OrOperation) current).getConditions();
                if (conditions != null) {
                    for (int i = conditions.size() - 1; i >= 0; i--) {
                        QueryOperation child = conditions.get(i);
                        if (child != null) {
                            pending.add(child);
                        }
                    }
                }
            } else if (current != null) {
                branches.add(current);
            }
        }
        return branches;
    }

    /**
     * Enforces DynamoDB's non-empty string primary-key invariant on collected seeds.
     * Rejects exact {@code ""} primary-key equalities and forces non-exact {@code ""} seeds to
     * {@code "NON_EMPTY_PRIMARY_KEY_FALLBACK"}.
     *
     * @param seeds collected attribute seeds (possibly updated in place)
     * @param primaryKeyNames base-table primary-key attribute names
     * @return {@code true} if all primary-key string seeds are valid or coercible, {@code false} to reject
     */
    public boolean applyPrimaryKeyEmptyStringGuard(CollectedSeeds seeds, Set<String> primaryKeyNames) {
        for (String keyName : primaryKeyNames) {
            DynamoDbAttributeValueDto existing = seeds.attributes.get(keyName);
            if (existing != null && existing.type == DynamoDbValueTypeDto.STRING && "".equals(existing.value)) {
                if (seeds.exactEqualityAttributes.contains(keyName)) {
                    return false;
                }
                seeds.attributes.put(
                        keyName,
                        new DynamoDbAttributeValueDto(keyName, DynamoDbKeyScalarTypeDto.STRING, NON_EMPTY_PRIMARY_KEY_FALLBACK));
            }
        }
        return true;
    }

    /**
     * Iteratively traverses {@code operation} in left-to-right pre-order across {@link AndOperation}
     * conjunctions, extracting flat scalar attribute seeds from supported leaf predicates
     * ({@code =}, {@code <}, {@code <=}, {@code >}, {@code >=}, {@code BETWEEN}, {@code IN}, and
     * {@code begins_with}) using first-value-wins precedence.
     * <p>
     * For {@link BetweenOperation}, attempts the lower bound before falling back to the upper bound;
     * for {@link InOperation}, takes the first convertible scalar candidate.
     * <p>
     * Any other {@link QueryOperation} node—specifically {@code OrOperation} (neither expanded nor
     * recursed into here; top-level {@code BatchGetItem} disjunctions are unpacked separately by
     * {@link #unpackBatchGetBranches}), {@code NotOperation}, {@code NotEqualsOperation} ({@code <>}),
     * {@code ContainsOperation}, {@code ExistsOperation}, {@code SizeOperation}, and
     * {@code TypeOperation}—is intentionally ignored without aborting sibling conjuncts.
     *
     * @param operation root predicate expression to traverse, or {@code null} (no-op)
     * @param attributes map populated in deterministic first-seen order with seeded attributes
     * @param exactEqualityAttributes set populated with attribute names whose winning seed came from {@code =}
     */
    private void collectOperation(
            QueryOperation operation,
            Map<String, DynamoDbAttributeValueDto> attributes,
            Set<String> exactEqualityAttributes) {
        if (operation == null) {
            return;
        }
        List<QueryOperation> pending = new ArrayList<>();
        pending.add(operation);
        while (!pending.isEmpty()) {
            QueryOperation current = pending.remove(pending.size() - 1);
            if (current instanceof AndOperation) {
                List<QueryOperation> conditions = ((AndOperation) current).getConditions();
                if (conditions != null) {
                    for (int i = conditions.size() - 1; i >= 0; i--) {
                        QueryOperation child = conditions.get(i);
                        if (child != null) {
                            pending.add(child);
                        }
                    }
                }
                continue;
            }
            if (current instanceof EqualsOperation<?>) {
                EqualsOperation<?> eq = (EqualsOperation<?>) current;
                putFirstSeed(eq.getFieldName(), eq.getValue(), true, attributes, exactEqualityAttributes);
            } else if (current instanceof LessThanOperation<?>) {
                LessThanOperation<?> lt = (LessThanOperation<?>) current;
                putFirstSeed(lt.getFieldName(), lt.getValue(), false, attributes, exactEqualityAttributes);
            } else if (current instanceof LessThanEqualsOperation<?>) {
                LessThanEqualsOperation<?> lte = (LessThanEqualsOperation<?>) current;
                putFirstSeed(lte.getFieldName(), lte.getValue(), false, attributes, exactEqualityAttributes);
            } else if (current instanceof GreaterThanOperation<?>) {
                GreaterThanOperation<?> gt = (GreaterThanOperation<?>) current;
                putFirstSeed(gt.getFieldName(), gt.getValue(), false, attributes, exactEqualityAttributes);
            } else if (current instanceof GreaterThanEqualsOperation<?>) {
                GreaterThanEqualsOperation<?> gte = (GreaterThanEqualsOperation<?>) current;
                putFirstSeed(gte.getFieldName(), gte.getValue(), false, attributes, exactEqualityAttributes);
            } else if (current instanceof BetweenOperation) {
                BetweenOperation between = (BetweenOperation) current;
                if (!putFirstSeed(between.getFieldName(), between.getLowerBound(), false, attributes, exactEqualityAttributes)) {
                    putFirstSeed(between.getFieldName(), between.getUpperBound(), false, attributes, exactEqualityAttributes);
                }
            } else if (current instanceof InOperation<?>) {
                InOperation<?> inOp = (InOperation<?>) current;
                List<?> values = inOp.getValues();
                if (values != null) {
                    for (Object candidate : values) {
                        if (putFirstSeed(inOp.getFieldName(), candidate, false, attributes, exactEqualityAttributes)) {
                            break;
                        }
                    }
                }
            } else if (current instanceof BeginsWithOperation) {
                BeginsWithOperation beginsWith = (BeginsWithOperation) current;
                putFirstSeed(beginsWith.getFieldName(), beginsWith.getPrefix(), false, attributes, exactEqualityAttributes);
            }
        }
    }

    /**
     * Validates and converts a candidate scalar seed, recording it under {@code fieldName} only if
     * no seed has been recorded for that attribute yet (first-value-wins).
     * <p>
     * Nested document/list paths ({@code a.b}, {@code a[0]}), blank names, and values rejected by
     * {@link DynamoDbDtoUtils#fromScalarValueOrNull} ({@code null}, non-scalars, non-finite numbers)
     * are ignored and cause this method to return {@code false}.
     *
     * @param fieldName candidate DynamoDB attribute name
     * @param value raw predicate operand value to convert
     * @param isExactEquality {@code true} if {@code value} came from an {@link EqualsOperation};
     *                        recorded in {@code exactEqualityAttributes} iff this call wins {@code fieldName}
     * @param attributes destination map of seeded attributes
     * @param exactEqualityAttributes destination set of attribute names whose winning seed came from {@code =}
     * @return {@code true} if {@code fieldName} and {@code value} form a valid flat scalar seed
     *         (even if an earlier predicate already seeded {@code fieldName}), {@code false} otherwise
     */
    private boolean putFirstSeed(
            String fieldName,
            Object value,
            boolean isExactEquality,
            Map<String, DynamoDbAttributeValueDto> attributes,
            Set<String> exactEqualityAttributes) {
        if (!isFlatAttributeName(fieldName)) {
            return false;
        }
        DynamoDbAttributeValueDto attribute = DynamoDbDtoUtils.fromScalarValueOrNull(fieldName, value);
        if (attribute == null) {
            return false;
        }
        if (attributes.putIfAbsent(fieldName, attribute) == null && isExactEquality) {
            exactEqualityAttributes.add(fieldName);
        }
        return true;
    }

    private static boolean isFlatAttributeName(String fieldName) {
        return fieldName != null && !fieldName.isEmpty() && !fieldName.contains(".") && !fieldName.contains("[");
    }
}
