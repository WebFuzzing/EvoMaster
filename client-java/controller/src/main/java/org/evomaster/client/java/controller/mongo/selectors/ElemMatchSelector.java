package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.isDocument;
/**
 * { field: { $elemMatch: { query1, query2, ... } } }
 */
public class ElemMatchSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (isDocument(value)) {
            QueryOperation condition = new QueryParser().parse(value);
            return new ElemMatchOperation(fieldName, condition);
        }
        return null;
    }

    @Override
    protected String operator() {
        return "$elemMatch";
    }
}