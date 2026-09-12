package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperationWithField;
import org.evomaster.client.java.controller.mongo.selectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Determines to which operation a query correspond.
 */
public class QueryParser {

    private static final String SYNTHETIC_FIELD_NAME = "$";
    private static final Set<String> COMMENTS_OPERATORS = new HashSet<>(Arrays.asList("$comment", "$comments"));

    List<QuerySelector> selectors = Arrays.asList(
            new EqualsSelector(),
            new NotEqualsSelector(),
            new LessThanEqualsSelector(),
            new LessThanSelector(),
            new GreaterThanSelector(),
            new GreaterThanEqualsSelector(),
            new AndSelector(),
            new OrSelector(),
            new NorSelector(),
            new InSelector(),
            new NotInSelector(),
            new AllSelector(),
            new SizeSelector(),
            new ElemMatchSelector(),
            new ModSelector(),
            new BitsAllClearSelector(),
            new BitsAllSetSelector(),
            new BitsAnyClearSelector(),
            new BitsAnySetSelector(),
            new NotSelector(),
            new ExistsSelector(),
            new TypeSelector(),
            new RegexSelector(),
            new NearSelector(),
            new NearSphereSelector(),
            new ImplicitSelector()
    );

    public QueryOperation parse(Object bsonDocument) {
        if (bsonDocument == null) {
            return null;
        }

        Object normalizedWithoutComments = removeCommentsOperators(bsonDocument);

        QueryOperation operation = parseWithSelectors(normalizedWithoutComments);
        if (operation != null && !usesOperatorAsFieldName(operation)) {
            return operation;
        }

        Object normalizedDocument = normalizeTopLevelValueOperatorQuery(normalizedWithoutComments);
        if (normalizedDocument == normalizedWithoutComments) {
            return operation;
        }
        return parseWithSelectors(normalizedDocument);
    }

    private Object removeCommentsOperators(Object bsonValue) {
        if (isBsonDocument(bsonValue)) {
            Object normalized = newDocument(bsonValue);
            for (String key : documentKeys(bsonValue)) {
                if (COMMENTS_OPERATORS.contains(key)) {
                    continue;
                }
                appendToDocument(normalized, key, removeCommentsOperators(getValue(bsonValue, key)));
            }
            return normalized;
        }

        if (bsonValue instanceof List<?>) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : (List<?>) bsonValue) {
                normalized.add(removeCommentsOperators(item));
            }
            return normalized;
        }

        return bsonValue;
    }

    private boolean usesOperatorAsFieldName(QueryOperation operation) {
        if (!(operation instanceof QueryOperationWithField)) {
            return false;
        }
        String fieldName = ((QueryOperationWithField) operation).getFieldName();
        return fieldName.startsWith(SYNTHETIC_FIELD_NAME)
                && !fieldName.equals(SYNTHETIC_FIELD_NAME);
    }

    private QueryOperation parseWithSelectors(Object bsonDocument) {
        List<QueryOperation> results = selectors.stream()
                .map(selector -> selector.getOperation(bsonDocument))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            return null;
        }

        if (results.size() > 1) {
            // If multiple selectors match, prioritize those that are NOT ImplicitSelector
            List<QueryOperation> nonImplicit = selectors.stream()
                    .filter(s -> !(s instanceof ImplicitSelector))
                    .map(selector -> selector.getOperation(bsonDocument))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (nonImplicit.size() == 1) {
                return nonImplicit.get(0);
            }
            // If still ambiguous or none, we might have a problem, but let's return the first one
            // or null if it's really ambiguous.
            // For now, return the first non-implicit, or first result.
            return nonImplicit.isEmpty() ? results.get(0) : nonImplicit.get(0);
        }

        return results.get(0);
    }

    private Object normalizeTopLevelValueOperatorQuery(Object bsonDocument) {
        if (!(bsonDocument instanceof Map) || !isBsonDocument(bsonDocument)) {
            return bsonDocument;
        }

        Set<String> keys = documentKeys(bsonDocument);
        if (keys == null || keys.isEmpty()
                || keys.stream().anyMatch(key -> !key.startsWith(SYNTHETIC_FIELD_NAME))) {
            return bsonDocument;
        }

        Object normalized = newDocument(bsonDocument);
        Object inner = newDocument(bsonDocument);
        for (String operator : keys) {
            appendToDocument(inner, operator, getValue(bsonDocument, operator));
        }
        appendToDocument(normalized, SYNTHETIC_FIELD_NAME, inner);
        return normalized;
    }
}
