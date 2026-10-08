package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass.getOriginal;

/**
 * Instrumentation for DynamoDB client classes.
 * Made the decision to add both Sync and Async replacements in one class.
 * They are mostly the same, but I couldn't find a better way with statics plus annotations limitations.
 */
public class DynamoDbClassReplacement {

    public static final String METHOD_TABLE_NAME = "tableName";
    public static final String METHOD_REQUEST_ITEMS = "requestItems";

    public static class Sync extends ThirdPartyMethodReplacementClass {
        public static final String DDB_GET_ITEM = "ddbGetItem";
        public static final String DDB_BATCH_GET_ITEM = "ddbBatchGetItem";
        public static final String DDB_PUT_ITEM = "ddbPutItem";
        public static final String DDB_UPDATE_ITEM = "ddbUpdateItem";
        public static final String DDB_DELETE_ITEM = "ddbDeleteItem";
        public static final String DDB_QUERY = "ddbQuery";
        public static final String DDB_SCAN = "ddbScan";
        private static final Sync singleton = new Sync();

        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.services.dynamodb.DynamoDbClient";
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.GetItemResponse")
        public static Object getItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.GetItemRequest") Object ddbRequest) {
            return handle(client, DDB_GET_ITEM, ddbRequest, DynamoDbOperationNames.GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object ddbRequest) {
            return handle(client, DDB_BATCH_GET_ITEM, ddbRequest, DynamoDbOperationNames.BATCH_GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.PutItemResponse")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object ddbRequest) {
            return handle(client, DDB_PUT_ITEM, ddbRequest, DynamoDbOperationNames.PUT_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object ddbRequest) {
            return handle(client, DDB_UPDATE_ITEM, ddbRequest, DynamoDbOperationNames.UPDATE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object ddbRequest) {
            return handle(client, DDB_DELETE_ITEM, ddbRequest, DynamoDbOperationNames.DELETE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.QueryResponse")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object ddbRequest) {
            return handle(client, DDB_QUERY, ddbRequest, DynamoDbOperationNames.QUERY);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.ScanResponse")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object ddbRequest) {
            return handle(client, DDB_SCAN, ddbRequest, DynamoDbOperationNames.SCAN);
        }
    }

    public static class Async extends ThirdPartyMethodReplacementClass {
        public static final String DDB_ASYNC_GET_ITEM = "ddbAsyncGetItem";
        public static final String DDB_ASYNC_BATCH_GET_ITEM = "ddbAsyncBatchGetItem";
        public static final String DDB_ASYNC_PUT_ITEM = "ddbAsyncPutItem";
        public static final String DDB_ASYNC_UPDATE_ITEM = "ddbAsyncUpdateItem";
        public static final String DDB_ASYNC_DELETE_ITEM = "ddbAsyncDeleteItem";
        public static final String DDB_ASYNC_QUERY = "ddbAsyncQuery";
        public static final String DDB_ASYNC_SCAN = "ddbAsyncScan";
        private static final Async singleton = new Async();

        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient";
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object getItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.GetItemRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_GET_ITEM, ddbRequest, DynamoDbOperationNames.GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_BATCH_GET_ITEM, ddbRequest, DynamoDbOperationNames.BATCH_GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_PUT_ITEM, ddbRequest, DynamoDbOperationNames.PUT_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_UPDATE_ITEM, ddbRequest, DynamoDbOperationNames.UPDATE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_DELETE_ITEM, ddbRequest, DynamoDbOperationNames.DELETE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_QUERY, ddbRequest, DynamoDbOperationNames.QUERY);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object ddbRequest) {
            return handleAsync(client, DDB_ASYNC_SCAN, ddbRequest, DynamoDbOperationNames.SCAN);
        }
    }

    /**
     * Invoke the original synchronous client method and trace the command execution.
     *
     * @param client DynamoDB Sync client object
     * @param id unique identifier for Evomaster to match the replacement method with the original one.
     * @param ddbRequest DynamoDB request object
     * @param operationName enum of the DynamoDB operation (e.g., "GetItem", "PutItem", etc.)
     *
     * @return the result of the original method invocation.
     */
    protected static Object handle(Object client, String id, Object ddbRequest, DynamoDbOperationNames operationName) {
        long start = System.currentTimeMillis();
        try {
            Method method = getOriginal(Sync.singleton, id, client);
            Object result = method.invoke(client, ddbRequest);

                long end = System.currentTimeMillis();
                List<String> tableNames = extractTableNames(ddbRequest);
                long executionTime = end - start;
                DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, ddbRequest, true, executionTime);
                ExecutionTracer.addDynamoDbInfo(info);
                return result;
            }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Invoke the original asynchronous client method and trace completion status.
     *
     * @param client DynamoDB Async client object
     * @param id unique identifier for Evomaster to match the replacement method with the original one.
     * @param ddbRequest DynamoDB request object
     * @param operationName enum of the DynamoDB operation (e.g., "GetItem", "PutItem", etc.)
     *
     * @return the result of the original method invocation.
     */
    protected static Object handleAsync(Object client, String id, Object ddbRequest, DynamoDbOperationNames operationName) {
        long start = System.currentTimeMillis();
        try {
            Method method = getOriginal(Async.singleton, id, client);
            Object result = method.invoke(client, ddbRequest);

            CompletableFuture<?> future = (CompletableFuture<?>) result;
            return future.handle((res, ex) -> {
                long end = System.currentTimeMillis();
                List<String> tableNames = extractTableNames(ddbRequest);
                boolean successful = ex == null;
                long executionTime = end - start;
                DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, ddbRequest, successful, executionTime);
                ExecutionTracer.addDynamoDbInfo(info);
                if (ex != null) {
                    if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                    throw new RuntimeException(ex);
                }
                return res;
            });
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Single entry point to extract table names from single-table and batch-table request types.
     *
     * @param ddbRequest DynamoDB request object
     *
     * @return a list containing a single table name (every operation except GetBatchItem) or list of table names, sorted alphabetically
     */
    private static List<String> extractTableNames(Object ddbRequest) {
        if (ddbRequest == null) return Collections.emptyList();

        //Assume we need to extract just one table name (every operation except GetBatchItem)
        String tableName = extractSingleTableName(ddbRequest);
        if (tableName != null) {
            return Collections.singletonList(tableName);
        }

        return extractBatchTableNames(ddbRequest);
    }

    /**
     * Extract table name from DynamoDB request objects that provide {@value METHOD_TABLE_NAME}.
     * Avoids throwing so {@code extractTableNames(Object)} can fall back to {@code extractBatchTableNames(Object)}
     *
     * @param ddbRequest DynamoDB request object
     *
     * @return a list containing a single table name (every operation except GetBatchItem) or {@code null} if reflection call fails.
     */
    private static String extractSingleTableName(Object ddbRequest) {
        try {
            Method getTableNameMethod = ddbRequest.getClass().getMethod(METHOD_TABLE_NAME);
            return (String) getTableNameMethod.invoke(ddbRequest);
        } catch (NoSuchMethodException ignored) {
            // Fail gracefully so the caller can fall back
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to retrieve table name from DynamoDB request", e);
        }
    }

    /**
     * Extract and sort table names from batchGetitem requests
     * @param ddbRequest DynamoDB request object
     *
     * @return a list of table names, sorted alphabetically
     */
    private static List<String> extractBatchTableNames(Object ddbRequest) {
        try {
            Method getRequestItemsMethod = ddbRequest.getClass().getMethod(METHOD_REQUEST_ITEMS);
            Object requestItems = getRequestItemsMethod.invoke(ddbRequest);
            if (!(requestItems instanceof Map)) {
                return Collections.emptyList();
            }

            List<String> tableNames = new ArrayList<>();
            for (Object key : ((Map<?, ?>) requestItems).keySet()) {
                if (key instanceof String) {
                    tableNames.add((String) key);
                }
            }

            Collections.sort(tableNames);
            return tableNames;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to retrieve table names from DynamoDB batch request", e);
        }
    }
}
