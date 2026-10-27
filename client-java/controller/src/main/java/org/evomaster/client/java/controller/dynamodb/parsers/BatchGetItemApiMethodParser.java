package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.OrOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbQuery;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbQueryValue;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;


/**
 * Parser for DynamoDB {@code BatchGetItem} requests.
 */
public class BatchGetItemApiMethodParser extends DynamoDbBaseApiMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.BATCH_GET_ITEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery) {
        Map<String, DynamoDbQuery.BatchGetKeys> batchRequestItems = ddbQuery.batchRequestItems();
        if (batchRequestItems == null) {
            SimpleLogger.uniqueWarn("Unexpected object type from requestItems in a BatchGetItem DynamoDB query");
            return Collections.emptyMap();
        }

        Map<String, ParsedDynamoDbRequest> result = new LinkedHashMap<>();
        for (Map.Entry<String, DynamoDbQuery.BatchGetKeys> entry : batchRequestItems.entrySet()) {
            String tableName = entry.getKey() == null ? null : entry.getKey();
            if (tableName == null || tableName.trim().isEmpty()) {
                SimpleLogger.uniqueWarn("Failed to parse tablename for BatchGetItem");
                continue;
            }

            List<Map<String, DynamoDbQueryValue>> keys = entry.getValue() == null ? null : entry.getValue().keys();
            if (keys == null) {
                SimpleLogger.uniqueWarn("Failed to parse keys for BatchGetItem for table: " + tableName);
                continue;
            }

            List<QueryOperation> keyConditions = new ArrayList<>();
            for (Map<String, DynamoDbQueryValue> rawKey : keys) {
                QueryOperation keyCondition = buildEqualsFromMap(DynamoDbAttributeValueHelper.toPlainMap(rawKey));
                if (keyCondition != null) {
                    keyConditions.add(keyCondition);
                }
            }

            QueryOperation tableOperation = combineWithOr(keyConditions);
            if (tableOperation != null) {
                result.put(tableName, new ParsedDynamoDbRequest(tableOperation, null));
            }
        }

        return result;
    }

    /**
     * Combines key conditions with OR semantics.
     *
     * @param conditions per-key conditions
     * @return combined operation
     */
    private QueryOperation combineWithOr(List<QueryOperation> conditions) {
        return combine(conditions, OrOperation::new);
    }
}
