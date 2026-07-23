package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.QueryParser;
import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Two possible implicit operations (no operator)
 * Equals: { field: value }
 * And: {{ expression1 }, { expression2 } , ... , { expressionN } }
 */
public class ImplicitSelector extends QuerySelector {

    private static final String PREFIX_OPERATOR = "$";

    @Override
    public QueryOperation getOperation(Object query) {
        if (!isImplicitQuery(query)) {
            return null;
        }

        Set<String> keys = documentKeys(query);
        if (keys.isEmpty()) {
            return new TrueOperation(); // Represents the "{}" MongoDB query, which matches all documents
        }

        if (!isUniqueEntry((Map<?, ?>) query)) {
            return handleMultipleFields(query);
        } else {
            return handleSingleField(query);
        }
    }

    private boolean isImplicitQuery(Object query) {
        if (!(query instanceof Map)) {
            return false;
        }

        Set<String> keys = documentKeys(query);
        if (keys == null) {
            return false;
        }

        if (keys.isEmpty()) {
            return true;
        }

        // If any key starts with $, this is not an implicit query
        return keys.stream().noneMatch(k -> k.startsWith(PREFIX_OPERATOR));
    }

    private QueryOperation handleMultipleFields(Object query) {
        List<QueryOperation> conditions = parseConditions(query);
        return (conditions == null || conditions.isEmpty()) ? null : new AndOperation(conditions);
    }

    private QueryOperation handleSingleField(Object query) {
        String fieldName = extractFieldName(query);
        Object value = getValue(query, fieldName);

        // If the value is a document, it might contain multiple operators (e.g. {age: {$gte: 18, $lt: 65}})
        // or it might be a literal document to match (e.g. {metadata: {foo: "bar"}})
        if (isBsonDocument(value)) {
            if (isEmptyDocument(value)) {
                return null;
            }
            QueryOperation multiOperatorOp = handlePotentialMultiOperatorValue(query, fieldName, value);
            if (multiOperatorOp != null || isBsonDocumentWithOperators(value)) {
                return multiOperatorOp;
            }
        }
        return new EqualsOperation<>(fieldName, value);
    }

    private boolean isEmptyDocument(Object value) {
        Set<String> keys = documentKeys(value);
        return keys == null || keys.isEmpty();
    }

    private QueryOperation handlePotentialMultiOperatorValue(Object query, String fieldName, Object value) {
        Set<String> innerKeys = documentKeys(value);
        if (innerKeys == null || innerKeys.isEmpty()) {
            return null;
        }

        // If it contains operators, they must all be operators
        boolean hasOperators = innerKeys.stream().anyMatch(k -> k.startsWith(PREFIX_OPERATOR));
        boolean allOperators = innerKeys.stream().allMatch(k -> k.startsWith(PREFIX_OPERATOR));

        if (hasOperators) {
            if (!allOperators) {
                return null; // Mixed operators and fields are not allowed in this position
            }
            if (innerKeys.size() > 1) {
                return handleMultiOperatorField(query, fieldName, value, innerKeys);
            }
            // If it's a single operator, let other selectors handle it (return null here)
            return null;
        }
        return null;
    }

    private boolean isBsonDocumentWithOperators(Object value) {
        if (!isBsonDocument(value)) {
            return false;
        }
        Set<String> keys = documentKeys(value);
        return keys != null && keys.stream().anyMatch(k -> k.startsWith(PREFIX_OPERATOR));
    }

    private QueryOperation handleMultiOperatorField(Object query, String fieldName, Object value, Set<String> operators) {
        // Multiple operators on the same field: { age: { $gte: 18, $lt: 65 } }
        // Split into multiple conditions: { age: { $gte: 18 } } and { age: { $lt: 65 } }
        List<QueryOperation> conditions = new ArrayList<>();
        for (String operator : operators) {
            Object operatorValue = getValue(value, operator);
            Object newQuery = newDocument(query);
            Object newInnerDoc = newDocument(value);
            appendToDocument(newInnerDoc, operator, operatorValue);
            appendToDocument(newQuery, fieldName, newInnerDoc);
            QueryOperation operation = new QueryParser().parse(newQuery);
            if (operation == null) {
                return null;
            }
            conditions.add(operation);
        }
        return new AndOperation(conditions);
    }

    protected List<QueryOperation> parseConditions(Object query) {
        Set<String> fields = documentKeys(query);
        ArrayList<QueryOperation> conditions = new ArrayList<>();
        if (fields == null) return conditions;
        for (String fieldName : fields) {
            Object newQuery = newDocument(query);
            appendToDocument(newQuery, fieldName, getValue(query, fieldName));
            QueryOperation operation = new QueryParser().parse(newQuery);
            if (operation == null) {
                return null; // All parts of an implicit AND must be valid
            }
            conditions.add(operation);
        }
        return conditions;
    }

    protected String extractOperator(Object query) {
        return "";
    }

    @Override
    protected String operator() {
        return "";
    }

    private String extractFieldName(Object query) {
        Set<String> keys = documentKeys(query);
        return keys == null ? null : keys.stream().findFirst().orElse(null);
    }
}
