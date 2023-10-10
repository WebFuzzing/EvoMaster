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
    @Override
    public QueryOperation getOperation(Object query) {
        if (!isUniqueEntry((Map<?, ?>) query)) {
            List<QueryOperation> conditions = parseConditions(query);
            return conditions.isEmpty()? null : new AndOperation(conditions);
        }else{
            String fieldName = extractFieldName(query);
            return new EqualsOperation<>(fieldName, getValue(query, fieldName));
        }
    }

    protected List<QueryOperation> parseConditions(Object query) {
        Set<String> fields = keySet(query);
        ArrayList<QueryOperation> conditions = new ArrayList<>();
        fields.forEach(fieldName -> {
            Object newQuery = newDocument(query);
            appendToDocument(newQuery, fieldName, getValue(query, fieldName));
            conditions.add(new QueryParser().parse(newQuery));
        });
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
        return keys.stream().findFirst().orElse(null);
    }
}