package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.DynamoDbCommand;
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
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass.getOriginal;

/**
 * Made the decision to add both Sync and Async replacements in one class. They are mostly the same but I couldn't find a better way with static and annotations
 */
public class DynamoDbClassReplacement {

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
            return handle(singleton, client, DDB_GET_ITEM, request, "GetItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object request) {
            return handle(singleton, client, DDB_BATCH_GET_ITEM, request, "BatchGetItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.PutItemResponse")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object request) {
            return handle(singleton, client, DDB_PUT_ITEM, request, "PutItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object request) {
            return handle(singleton, client, DDB_UPDATE_ITEM, request, "UpdateItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object request) {
            return handle(singleton, client, DDB_DELETE_ITEM, request, "DeleteItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.QueryResponse")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object request) {
            return handle(singleton, client, DDB_QUERY, request, "Query");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.services.dynamodb.model.ScanResponse")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object request) {
            return handle(singleton, client, DDB_SCAN, request, "Scan");
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
            return handle(singleton, client, DDB_ASYNC_GET_ITEM, request, "GetItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_BATCH_GET_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object batchGetItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_BATCH_GET_ITEM, request, "BatchGetItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_PUT_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object putItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.PutItemRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_PUT_ITEM, request, "PutItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_UPDATE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object updateItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_UPDATE_ITEM, request, "UpdateItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_DELETE_ITEM, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object deleteItem(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_DELETE_ITEM, request, "DeleteItem");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_QUERY, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object query(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.QueryRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_QUERY, request, "Query");
        }

        @Replacement(type = ReplacementType.TRACKER, id = DDB_ASYNC_SCAN, usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB, castTo = "java.util.concurrent.CompletableFuture")
        public static Object scan(Object client, @ThirdPartyCast(actualType = "software.amazon.awssdk.services.dynamodb.model.ScanRequest") Object request) {
            return handle(singleton, client, DDB_ASYNC_SCAN, request, "Scan");
        }
    }

    protected static Object handle(ThirdPartyMethodReplacementClass singleton, Object client, String id, Object request, String operationName) {
        long start = System.currentTimeMillis();
        boolean isAsync = client.getClass().getName().contains("Async");
        try {
            Method method = getOriginal(singleton, id, client);
            Object result = method.invoke(client, request);

            if (isAsync) {
                CompletableFuture<?> future = (CompletableFuture<?>) result;
                return future.handle((res, ex) -> {
                    long end = System.currentTimeMillis();
                    List<String> tableNames = extractTableNames(request);
                    boolean successful = ex == null;
                    long executionTime = end - start;
                    DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, request, successful, executionTime);
                    ExecutionTracer.addDynamoDbInfo(info);
                    logInterception(id, operationName, isAsync, tableNames, request, successful, executionTime, ex);
                    if (ex != null) {
                        if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                        throw new RuntimeException(ex);
                    }
                    return res;
                });
            } else {
                long end = System.currentTimeMillis();
                List<String> tableNames = extractTableNames(request);
                long executionTime = end - start;
                DynamoDbCommand info = new DynamoDbCommand(tableNames, operationName, request, true, executionTime);
                ExecutionTracer.addDynamoDbInfo(info);
                return result;
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static List<String> extractTableNames(Object request) {
        if (request == null) return Collections.emptyList();

        try {
            Method getTableNameMethod = request.getClass().getMethod("tableName");
            String tableName = (String) getTableNameMethod.invoke(request);
            if (tableName != null) {
                return Collections.singletonList(tableName);
            }
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Collections.emptyList();
        }

        try {
            Method getRequestItemsMethod = request.getClass().getMethod("requestItems");
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
            return Collections.emptyList();
        }
    }
}
