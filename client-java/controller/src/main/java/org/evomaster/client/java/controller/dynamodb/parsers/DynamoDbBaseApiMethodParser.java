package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.DynamoDbExpressionParser;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;

import java.util.*;
import java.util.function.Function;

import static org.evomaster.client.java.controller.dynamodb.DynamoDbReflectionHelper.invokeNoArg;

/**
 * Base class for DynamoDB SDK requests parser. Contains shared utilities.
 */
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

    /**
     * Parses a DynamoDB expression string into a query operation.
     *
     * @param expression expression string
     * @param expressionAttributeNames name placeholders map
     * @param expressionAttributeValues value placeholders map
     * @return parsed operation, or {@code null}
     */
    protected QueryOperation parseExpression(
            String expression,
            Map<String, String> expressionAttributeNames,
            Map<String, Object> expressionAttributeValues) {
        return new DynamoDbExpressionParser().parse(expression, expressionAttributeNames, expressionAttributeValues);
    }

    /**
     * Parses key equality conditions from request key fields.
     *
     * @param request request object
     * @return parsed key condition operation
     */
    protected QueryOperation parseKeyCondition(Object request) {
        Object keyObj = invokeNoArg(request, METHOD_KEY);
        return buildEqualsFromMap(DynamoDbAttributeValueHelper.toPlainMap(keyObj));
    }

    /**
     * Builds equality operations from a field/value map.
     *
     * @param values field/value map
     * @return combined equality operation, or {@code null}
     */
    protected QueryOperation buildEqualsFromMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<QueryOperation> conditions = new ArrayList<>();
        values.forEach((key, value) -> conditions.add(new EqualsOperation<>(key, value)));
        return combineWithAnd(conditions);
    }

    /**
     * Combines two operations with AND semantics.
     *
     * @param left left operation
     * @param right right operation
     * @return combined operation
     */
    protected QueryOperation combineWithAnd(QueryOperation left, QueryOperation right) {
        return combineWithAnd(Arrays.asList(left, right));
    }

    /**
     * Combines a list of operations with AND semantics.
     *
     * @param conditions conditions to combine
     * @return combined operation
     */
    protected QueryOperation combineWithAnd(List<QueryOperation> conditions) {
        return combine(conditions, AndOperation::new);
    }

    /**
     * Combines a list of operations with a provided composite builder, skipping null entries.
     *
     * @param conditions operations to combine
     * @param compositeBuilder builder for composite operation
     * @return combined operation, one operation, or {@code null}
     */
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

    /**
     * Reads and normalizes expression attribute names from request object.
     *
     * @param request request object
     * @return normalized name map
     */
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

    /**
     * Reads and converts expression attribute values from request object.
     *
     * @param request request object
     * @return normalized value map
     */
    protected Map<String, Object> readValueMap(Object request) {
        Object raw = invokeNoArg(request, METHOD_EXPRESSION_ATTRIBUTE_VALUES);
        return DynamoDbAttributeValueHelper.toPlainMap(raw);
    }

    /**
     * Reads a string-like value from request object via reflection.
     *
     * @param target target object
     * @param methodName accessor method name
     * @return string value, or {@code null}
     */
    protected String readString(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Reads and validates the table name from the request.
     *
     * @param request request object
     * @return table name, or {@code null} if blank/absent
     */
    protected String readValidTableName(Object request) {
        String tableName = readString(request, METHOD_TABLE_NAME);
        if (tableName == null || tableName.trim().isEmpty()) {
            return null;
        }
        return tableName;
    }

    /**
     * Builds a singleton result map for one table/operation pair.
     *
     * @param tableName table name
     * @param operation parsed operation
     * @return singleton map or empty map when invalid
     */
    protected Map<String, QueryOperation> singleTableResult(String tableName, QueryOperation operation) {
        if (tableName == null || operation == null) {
            return Collections.emptyMap();
        }

        Map<String, QueryOperation> result = new LinkedHashMap<>();
        result.put(tableName, operation);
        return result;
    }
}
