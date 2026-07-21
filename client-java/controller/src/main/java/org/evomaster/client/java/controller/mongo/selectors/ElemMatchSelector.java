package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.Objects;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.isBsonDocument;
/**
 * { field: { $elemMatch: { query1, query2, ... } } }
 */
public class ElemMatchSelector extends SingleConditionQuerySelector {

    public static final String ELEM_MATCH_OPERATOR = "$elemMatch";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (isBsonDocument(value)) {
            QueryOperation condition = new QueryParser().parse(value);
            return new ElemMatchOperation(fieldName, condition);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return ELEM_MATCH_OPERATOR;
    }
}
