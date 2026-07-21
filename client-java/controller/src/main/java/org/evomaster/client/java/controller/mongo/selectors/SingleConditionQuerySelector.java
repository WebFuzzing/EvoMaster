package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Selectors for operations whose value consist of a single condition
 */
abstract class SingleConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object bsonDocument) {
        Objects.requireNonNull(bsonDocument);

        if (!(bsonDocument instanceof Map)) {
            return null;
        }

        String fieldName = extractFieldName(bsonDocument);
        if (fieldName == null) {
            return null;
        }

        if (!isUniqueEntry((Map<?, ?>) bsonDocument)) {
            return null;
        }

        Object innerDoc = getValue(bsonDocument, fieldName);
        if (!isBsonDocument(innerDoc) || !hasTheExpectedOperator(bsonDocument)) {
            return null;
        }

        Set<String> innerKeys = documentKeys(innerDoc);
        if (innerKeys == null || innerKeys.size() != 1) {
            return null;
        }

        if (innerKeys.contains(operator())) {
            Object value = getValue(innerDoc, operator());
            return parseValue(fieldName, value);
        } else {
            return null;
        }

    }

    protected String extractOperator(Object query) {
        String fieldName = extractFieldName(query);
        if (fieldName == null) return null;
        Set<String> keys = documentKeys(getValue(query, fieldName));
        return keys == null ? null : keys.stream().findFirst().orElse(null);
    }

    protected abstract QueryOperation parseValue(String fieldName, Object value);

    private String extractFieldName(Object query) {
        Set<String> keys = documentKeys(query);
        return keys == null ? null : keys.stream().findFirst().orElse(null);
    }
}
