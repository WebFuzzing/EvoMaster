package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.controller.dynamodb.operations.OrOperation;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.*;

import static org.evomaster.client.java.controller.dynamodb.DynamoDbReflectionHelper.invokeNoArg;

public class BatchGetItemApiMethodParser extends DynamoDbBaseApiMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.BATCH_GET_ITEM;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, QueryOperation> parseRequest(Object request) {
        Object requestItemsObj = invokeNoArg(request, METHOD_REQUEST_ITEMS);
        if (!(requestItemsObj instanceof Map<?, ?>)) {
            return Collections.emptyMap();
        }

        Map<String, QueryOperation> result = new LinkedHashMap<>();
        Map<?, ?> requestItems = (Map<?, ?>) requestItemsObj;
        for (Map.Entry<?, ?> entry : requestItems.entrySet()) {
            String tableName = entry.getKey() == null ? null : String.valueOf(entry.getKey());
            if (tableName == null || tableName.trim().isEmpty()) {
                continue;
            }

            Object keysAndAttributes = entry.getValue();
            Object keysObj = invokeNoArg(keysAndAttributes, METHOD_KEYS);
            if (!(keysObj instanceof Collection<?>)) {
                continue;
            }

            List<QueryOperation> keyConditions = new ArrayList<>();
            for (Object rawKey : (Collection<Object>) keysObj) {
                QueryOperation keyCondition = buildEqualsFromMap(DynamoDbAttributeValueHelper.toPlainMap(rawKey));
                if (keyCondition != null) {
                    keyConditions.add(keyCondition);
                }
            }

            QueryOperation tableOperation = combineWithOr(keyConditions);
            if (tableOperation != null) {
                result.put(tableName, tableOperation);
            }
        }

        return result;
    }

    private QueryOperation combineWithOr(List<QueryOperation> conditions) {
        return combine(conditions, OrOperation::new);
    }
}
