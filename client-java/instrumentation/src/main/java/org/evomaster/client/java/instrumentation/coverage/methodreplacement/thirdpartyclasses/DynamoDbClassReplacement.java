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
 * They are mostly the same, but I couldn't find a better way with statics plus annotations
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
        public static Object getItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.GetItemRequest") Object request) {
            return handle(client, DDB_GET_ITEM, request, DynamoDbOperationNames.GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object request) {
            return handle(client, DDB_BATCH_GET_ITEM, request, DynamoDbOperationNames.BATCH_GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.PutItemResponse")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object request) {
            return handle(client, DDB_PUT_ITEM, request, DynamoDbOperationNames.PUT_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object request) {
            return handle(client, DDB_UPDATE_ITEM, request, DynamoDbOperationNames.UPDATE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object request) {
            return handle(client, DDB_DELETE_ITEM, request, DynamoDbOperationNames.DELETE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.QueryResponse")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object request) {
            return handle(client, DDB_QUERY, request, DynamoDbOperationNames.QUERY);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.ScanResponse")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object request) {
            return handle(client, DDB_SCAN, request, DynamoDbOperationNames.SCAN);
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
        public static Object getItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.GetItemRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_GET_ITEM, request, DynamoDbOperationNames.GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_BATCH_GET_ITEM, request, DynamoDbOperationNames.BATCH_GET_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_PUT_ITEM, request, DynamoDbOperationNames.PUT_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_UPDATE_ITEM, request, DynamoDbOperationNames.UPDATE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_DELETE_ITEM, request, DynamoDbOperationNames.DELETE_ITEM);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_QUERY, request, DynamoDbOperationNames.QUERY);
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object request) {
            return handleAsync(client, DDB_ASYNC_SCAN, request, DynamoDbOperationNames.SCAN);
        }
    }

    /**
     * Invoke the original synchronous client method and trace the command execution.
     */
    protected static Object handle(Object client, String id, Object request, DynamoDbOperationNames operationName) {
        long start = System.currentTimeMillis();
        try {
            Method method = getOriginal(Sync.singleton, id, client);
            Object result = method.invoke(client, request);

                long end = System.currentTimeMillis();
                List<String> tableNames = extractTableNames(request);
                long executionTime = end - start;
                DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, request, true, executionTime);
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
     */
    protected static Object handleAsync(Object client, String id, Object request, DynamoDbOperationNames operationName) {
        long start = System.currentTimeMillis();
        try {
            Method method = getOriginal(Async.singleton, id, client);
            Object result = method.invoke(client, request);

            CompletableFuture<?> future = (CompletableFuture<?>) result;
            return future.handle((res, ex) -> {
                long end = System.currentTimeMillis();
                List<String> tableNames = extractTableNames(request);
                boolean successful = ex == null;
                long executionTime = end - start;
                DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, request, successful, executionTime);
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
     * Extract table names from single-table and batch-table request types.
     */
    private static List<String> extractTableNames(Object request) {
        if (request == null) return Collections.emptyList();

        //Assume it's GetItem first so try to extract single table name
        String tableName = extractSingleTableName(request);
        if (tableName != null) {
            return Collections.singletonList(tableName);
        }

        return extractBatchTableNames(request);
    }

    /**
     * Extract table name from request objects that provide {@code tableName()}.
     */
    private static String extractSingleTableName(Object request) {
        try {
            Method getTableNameMethod = request.getClass().getMethod(METHOD_TABLE_NAME);
            return (String) getTableNameMethod.invoke(request);
        } catch (NoSuchMethodException ignored) {
            // Ignore as BatchGetItem requests do not have tableName and are handled by extractBatchTableNames.
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to retrieve table name from DynamoDB request", e);
        }
    }

    /**
     * Extract and sort table names from batchGetitem requests
     */
    private static List<String> extractBatchTableNames(Object request) {
        try {
            Method getRequestItemsMethod = request.getClass().getMethod(METHOD_REQUEST_ITEMS);
            Object requestItems = getRequestItemsMethod.invoke(request);
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
