package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.QueryParser;
import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.*;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Selectors for operations whose value consist of a list of conditions
 */
abstract public class MultiConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object bsonDocument) {
        Objects.requireNonNull(bsonDocument);

        if (!hasTheExpectedOperator(bsonDocument)) {
            return null;
        }

        Object value = getValue(bsonDocument, operator());
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?>)) {
            return null;
        }

        ArrayList<QueryOperation> conditions = parseConditions((List<?>) value);
        if (conditions == null) {
            return null;
        } else {
            return composeConditions(conditions);
        }
    }

    @Override
    protected String extractOperator(Object query) {
        Objects.requireNonNull(query);
        if (!isBsonDocument(query)) {
            return null;
        } else {
            Set<String> keys = documentKeys(query);
            return keys == null ? null : keys.stream().findFirst().orElse(null);
        }
    }

    private static ArrayList<QueryOperation> parseConditions(List<?> value) {
        ArrayList<QueryOperation> conditions = new ArrayList<>();
        for (Object condition : value) {
            QueryOperation operation = new QueryParser().parse(condition);
            if (operation == null) {
                return null;
            }
            conditions.add(operation);
        }
        return conditions;
    }

    /**
     * Composes a {@link QueryOperation} from a list of conditions. The specific behavior
     * of this method depends on the implementation in subclasses and how they process
     * the provided conditions to create a composite operation.
     *
     * @param conditions the list of {@link QueryOperation} objects representing the individual query conditions
     * @return a {@link QueryOperation} representing the composed query based on the given conditions,
     * or null if the conditions cannot be composed
     */
    protected abstract QueryOperation composeConditions(List<QueryOperation> conditions);


}
