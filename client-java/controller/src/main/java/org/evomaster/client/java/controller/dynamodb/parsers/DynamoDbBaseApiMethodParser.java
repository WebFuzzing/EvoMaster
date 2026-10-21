package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.DynamoDbExpressionParser;
import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.AndOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbRequestSnapshot;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.EqualsOperation;

import java.util.*;
import java.util.function.Function;


/**
 * Base class for parsing DynamoDB request snapshots. Contains shared utilities.
 */
abstract class DynamoDbBaseApiMethodParser implements DynamoDbApiMethodParser {

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
     * Parses key equality conditions from DynamoDB request key fields.
     *
     * @param ddbRequest DynamoDB request snapshot
     * @return parsed key condition operation
     */
    protected QueryOperation parseKeyCondition(DynamoDbRequestSnapshot ddbRequest) {
        return buildEqualsFromMap(DynamoDbAttributeValueHelper.toPlainMap(ddbRequest.key()));
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
     * Reads and normalizes expression attribute names from a DynamoDB request snapshot.
     *
     * @param ddbRequest DynamoDB request snapshot
     * @return normalized name map
     */
    protected Map<String, String> readNameMap(DynamoDbRequestSnapshot ddbRequest) {
        Map<String, String> names = ddbRequest.expressionAttributeNames();
        return names == null ? Collections.emptyMap() : names;
    }

    /**
     * Reads and converts expression attribute values from a DynamoDB request snapshot.
     *
     * @param ddbRequest DynamoDB request snapshot
     * @return normalized value map
     */
    protected Map<String, Object> readValueMap(DynamoDbRequestSnapshot ddbRequest) {
        return DynamoDbAttributeValueHelper.toPlainMap(ddbRequest.expressionAttributeValues());
    }

    /**
     * Reads and validates the table name from the request.
     *
     * @param ddbRequest DynamoDB request snapshot
     * @return table name, or {@code null} if blank/absent
     */
    protected String readValidTableName(DynamoDbRequestSnapshot ddbRequest) {
        String tableName = ddbRequest.tableName();
        if (tableName == null || tableName.trim().isEmpty()) {
            return null;
        }
        return tableName;
    }

    /**
     * Builds a singleton result map for one table/request pair.
     *
     * @param tableName table name
     * @param keyCondition parsed key condition
     * @param filterExpression parsed filter or condition expression
     * @return singleton map or empty map when invalid
     */
    protected Map<String, ParsedDynamoDbRequest> buildSingleTableRequestMap(
            String tableName,
            QueryOperation keyCondition,
            QueryOperation filterExpression) {
        if (tableName == null || (keyCondition == null && filterExpression == null)) {
            return Collections.emptyMap();
        }

        Map<String, ParsedDynamoDbRequest> result = new LinkedHashMap<>();
        result.put(tableName, new ParsedDynamoDbRequest(keyCondition, filterExpression));
        return result;
    }
}
