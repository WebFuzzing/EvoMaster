package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.DynamoDbExpressionParser;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;

import java.util.*;
import java.util.function.Function;

import static org.evomaster.client.java.controller.dynamodb.DynamoDbReflectionHelper.invokeNoArg;

abstract class DynamoDbBaseApiMethodParser implements DynamoDbApiMethodParser {

    // All these constants are used to invoke DDB API methods by reflection, do not change.
    protected static final String METHOD_TABLE_NAME = "tableName";
    protected static final String METHOD_KEY_CONDITION_EXPRESSION = "keyConditionExpression";
    protected static final String METHOD_FILTER_EXPRESSION = "filterExpression";
    protected static final String METHOD_KEY = "key";
    protected static final String METHOD_REQUEST_ITEMS = "requestItems";
    protected static final String METHOD_KEYS = "keys";
    protected static final String METHOD_CONDITION_EXPRESSION = "conditionExpression";
    protected static final String METHOD_EXPRESSION_ATTRIBUTE_NAMES = "expressionAttributeNames";
    protected static final String METHOD_EXPRESSION_ATTRIBUTE_VALUES = "expressionAttributeValues";

    protected QueryOperation parseExpression(
            String expression,
            Map<String, String> expressionAttributeNames,
            Map<String, Object> expressionAttributeValues) {
        return new DynamoDbExpressionParser().parse(expression, expressionAttributeNames, expressionAttributeValues);
    }

    protected QueryOperation parseKeyCondition(Object request) {
        Object keyObj = invokeNoArg(request, METHOD_KEY);
        return buildEqualsFromMap(DynamoDbAttributeValueHelper.toPlainMap(keyObj));
    }

    protected QueryOperation buildEqualsFromMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<QueryOperation> conditions = new ArrayList<>();
        values.forEach((key, value) -> conditions.add(new EqualsOperation<>(key, value)));
        return combineWithAnd(conditions);
    }

    protected QueryOperation combineWithAnd(QueryOperation left, QueryOperation right) {
        return combineWithAnd(Arrays.asList(left, right));
    }

    protected QueryOperation combineWithAnd(List<QueryOperation> conditions) {
        return combine(conditions, AndOperation::new);
    }

    protected QueryOperation combine(List<QueryOperation> conditions, Function<List<QueryOperation>, QueryOperation> compositeBuilder) {
        List<QueryOperation> filtered = new ArrayList<>();
        for (QueryOperation operation : conditions) {
            if (operation != null) {
                filtered.add(operation);
            }
        }

        if (filtered.isEmpty()) {
            return null;
        }
        if (filtered.size() == 1) {
            return filtered.get(0);
        }
        return compositeBuilder.apply(filtered);
    }

    protected Map<String, String> readNameMap(Object request) {
        Object raw = invokeNoArg(request, METHOD_EXPRESSION_ATTRIBUTE_NAMES);
        if (!(raw instanceof Map<?, ?>)) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        ((Map<?, ?>) raw).forEach((k, v) -> {
            if (k != null && v != null) {
                result.put(String.valueOf(k), String.valueOf(v));
            }
        });
        return result;
    }

    protected Map<String, Object> readValueMap(Object request) {
        Object raw = invokeNoArg(request, METHOD_EXPRESSION_ATTRIBUTE_VALUES);
        return DynamoDbAttributeValueHelper.toPlainMap(raw);
    }

    protected String readString(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value == null ? null : String.valueOf(value);
    }

    protected String readValidTableName(Object request) {
        String tableName = readString(request, METHOD_TABLE_NAME);
        if (tableName == null || tableName.trim().isEmpty()) {
            return null;
        }
        return tableName;
    }

    protected Map<String, QueryOperation> singleTableResult(String tableName, QueryOperation operation) {
        if (tableName == null || operation == null) {
            return Collections.emptyMap();
        }

        Map<String, QueryOperation> result = new LinkedHashMap<>();
        result.put(tableName, operation);
        return result;
    }
}
