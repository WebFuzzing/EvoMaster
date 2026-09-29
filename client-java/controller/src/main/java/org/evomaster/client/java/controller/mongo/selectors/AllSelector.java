package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.QueryParser;
import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.documentKeys;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getValue;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.isBsonDocument;

/**
 * Represents a selector for the MongoDB `$all` operator.
 * { field: { $all: [ value1 , value2 ... ] } }
 *
 * <p>
 * This operator matches arrays that contain all elements specified in the query.
 * The selector checks if the query value is a list and, if so, creates an
 * {@link AllOperation} object corresponding to the field and the list of values.
 * Elements shaped as { $elemMatch: {...} } are a documented exception: rather than
 * being compared for equality, they are parsed into an {@link ElemMatchOperation}
 * that must be satisfied by some element of the array.
 */
public class AllSelector extends SingleConditionQuerySelector {

    public static final String ALL_OPERATOR = "$all";
    private static final String ELEM_MATCH_OPERATOR = "$elemMatch";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof List<?>) {
            List<Object> parsedValues = new ArrayList<>();
            for (Object element : (List<?>) value) {
                parsedValues.add(parseElement(fieldName, element));
            }
            return new AllOperation<>(fieldName, parsedValues);
        } else {
            return null;
        }
    }

    private Object parseElement(String fieldName, Object element) {
        if (!isBsonDocument(element)) {
            return element;
        }

        Set<String> keys = documentKeys(element);
        if (keys == null || keys.size() != 1 || !keys.contains(ELEM_MATCH_OPERATOR)) {
            return element;
        }

        Object innerQuery = getValue(element, ELEM_MATCH_OPERATOR);
        if (!isBsonDocument(innerQuery)) {
            return element;
        }

        QueryOperation condition = new QueryParser().parse(innerQuery);
        return condition == null ? element : new ElemMatchOperation(fieldName, condition);
    }

    @Override
    protected String operator() {
        return ALL_OPERATOR;
    }
}
