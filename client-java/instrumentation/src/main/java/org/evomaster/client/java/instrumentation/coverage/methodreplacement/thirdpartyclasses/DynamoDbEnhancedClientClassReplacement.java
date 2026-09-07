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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass.getOriginal;

/**
 * Method replacements for the synchronous and asynchronous DynamoDB enhanced clients and tables.
 */
public final class DynamoDbEnhancedClientClassReplacement {

    private static final String COMPLETABLE_FUTURE = "java.util.concurrent.CompletableFuture";
    private static final String PAGE_ITERABLE =
            "software.amazon.awssdk.enhanced.dynamodb.model.PageIterable";
    private static final String PAGE_PUBLISHER =
            "software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher";

    private DynamoDbEnhancedClientClassReplacement() {
    }

    /**
     * Replacements for {@code DynamoDbTable} calls.
     */
    public static class SyncTable extends ThirdPartyMethodReplacementClass {

        private static final String GET_ITEM_KEY = "enhancedSyncGetItemKey";
        private static final String GET_ITEM_REQUEST = "enhancedSyncGetItemRequest";
        private static final String PUT_ITEM_OBJECT = "enhancedSyncPutItemObject";
        private static final String PUT_ITEM_REQUEST = "enhancedSyncPutItemRequest";
        private static final String UPDATE_ITEM_OBJECT = "enhancedSyncUpdateItemObject";
        private static final String UPDATE_ITEM_REQUEST = "enhancedSyncUpdateItemRequest";
        private static final String DELETE_ITEM_KEY = "enhancedSyncDeleteItemKey";
        private static final String DELETE_ITEM_REQUEST = "enhancedSyncDeleteItemRequest";
        private static final String QUERY_CONSUMER = "enhancedSyncQueryConsumer";
        private static final String QUERY_REQUEST = "enhancedSyncQueryRequest";
        private static final String SCAN_CONSUMER = "enhancedSyncScanConsumer";
        private static final String SCAN_REQUEST = "enhancedSyncScanRequest";
        private static final SyncTable singleton = new SyncTable();

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable";
        }

        /**
         * Intercepts a synchronous enhanced get by key.
         *
         * @param table enhanced table
         * @param key item key
         * @return mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = GET_ITEM_KEY, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object getItem(Object table,
                                     @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.Key")
                                     Object key) {
            return invokeEnhanced(singleton, GET_ITEM_REQUEST, table, DynamoDbOperationNames.GET_ITEM, key);
        }

        /**
         * Intercepts a synchronous enhanced get request.
         *
         * @param table enhanced table
         * @param request enhanced get request
         * @return mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = GET_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object getItem_EM_0(Object table,
                                          @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest")
                                          Object request) {
            return invoke(singleton, GET_ITEM_REQUEST, table, DynamoDbOperationNames.GET_ITEM, request);
        }

        /**
         * Intercepts a synchronous enhanced put using a mapped item.
         *
         * @param table enhanced table
         * @param item mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = PUT_ITEM_OBJECT, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static void putItem(Object table, Object item) {
            invokeEnhanced(singleton, PUT_ITEM_REQUEST, table, DynamoDbOperationNames.PUT_ITEM, item);
        }

        /**
         * Intercepts a synchronous enhanced put request.
         *
         * @param table enhanced table
         * @param request enhanced put request
         */
        @Replacement(type = ReplacementType.TRACKER, id = PUT_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static void putItem_EM_0(Object table,
                                        @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest")
            Object request) {
            invoke(singleton, PUT_ITEM_REQUEST, table, DynamoDbOperationNames.PUT_ITEM, request);
        }

        /**
         * Intercepts a synchronous enhanced update using a mapped item.
         *
         * @param table enhanced table
         * @param item mapped item
         * @return updated mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = UPDATE_ITEM_OBJECT, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object updateItem(Object table, Object item) {
            return invokeEnhanced(singleton, UPDATE_ITEM_REQUEST, table, DynamoDbOperationNames.UPDATE_ITEM, item);
        }

        /**
         * Intercepts a synchronous enhanced update request.
         *
         * @param table enhanced table
         * @param request enhanced update request
         * @return updated mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = UPDATE_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object updateItem_EM_0(Object table,
                                             @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest")
                                             Object request) {
            return invoke(singleton, UPDATE_ITEM_REQUEST, table, DynamoDbOperationNames.UPDATE_ITEM, request);
        }

        /**
         * Intercepts a synchronous enhanced delete by key.
         *
         * @param table enhanced table
         * @param key item key
         * @return deleted mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = DELETE_ITEM_KEY, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object deleteItem(Object table,
                                        @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.Key")
                                        Object key) {
            return invokeEnhanced(singleton, DELETE_ITEM_REQUEST, table, DynamoDbOperationNames.DELETE_ITEM, key);
        }

        /**
         * Intercepts a synchronous enhanced delete request.
         *
         * @param table enhanced table
         * @param request enhanced delete request
         * @return deleted mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = DELETE_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB)
        public static Object deleteItem_EM_0(Object table,
                                             @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest")
                                             Object request) {
            return invoke(singleton, DELETE_ITEM_REQUEST, table, DynamoDbOperationNames.DELETE_ITEM, request);
        }

        /**
         * Intercepts a synchronous enhanced query configured by a consumer.
         *
         * @param table enhanced table
         * @param requestConsumer query request builder consumer
         * @return query pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = QUERY_CONSUMER, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_ITERABLE)
        public static Object query(Object table, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, QUERY_REQUEST, table, DynamoDbOperationNames.QUERY, requestConsumer);
        }

        /**
         * Intercepts a synchronous enhanced query request.
         *
         * @param table enhanced table
         * @param request enhanced query request
         * @return query pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = QUERY_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_ITERABLE)
        public static Object query_EM_0(Object table,
                                        @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest")
                                        Object request) {
            return invoke(singleton, QUERY_REQUEST, table, DynamoDbOperationNames.QUERY, request);
        }

        /**
         * Intercepts a synchronous enhanced scan configured by a consumer.
         *
         * @param table enhanced table
         * @param requestConsumer scan request builder consumer
         * @return scan pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = SCAN_CONSUMER, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_ITERABLE)
        public static Object scan(Object table, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, SCAN_REQUEST, table, DynamoDbOperationNames.SCAN, requestConsumer);
        }

        /**
         * Intercepts a synchronous enhanced scan request.
         *
         * @param table enhanced table
         * @param request enhanced scan request
         * @return scan pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = SCAN_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_ITERABLE)
        public static Object scan_EM_0(Object table,
                                       @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest")
                                       Object request) {
            return invoke(singleton, SCAN_REQUEST, table, DynamoDbOperationNames.SCAN, request);
        }
    }

    /**
     * Replacements for {@code DynamoDbAsyncTable} calls.
     */
    public static class AsyncTable extends ThirdPartyMethodReplacementClass {

        private static final String GET_ITEM_KEY = "enhancedAsyncGetItemKey";
        private static final String GET_ITEM_REQUEST = "enhancedAsyncGetItemRequest";
        private static final String PUT_ITEM_OBJECT = "enhancedAsyncPutItemObject";
        private static final String PUT_ITEM_REQUEST = "enhancedAsyncPutItemRequest";
        private static final String UPDATE_ITEM_OBJECT = "enhancedAsyncUpdateItemObject";
        private static final String UPDATE_ITEM_REQUEST = "enhancedAsyncUpdateItemRequest";
        private static final String DELETE_ITEM_KEY = "enhancedAsyncDeleteItemKey";
        private static final String DELETE_ITEM_REQUEST = "enhancedAsyncDeleteItemRequest";
        private static final String QUERY_CONSUMER = "enhancedAsyncQueryConsumer";
        private static final String QUERY_REQUEST = "enhancedAsyncQueryRequest";
        private static final String SCAN_CONSUMER = "enhancedAsyncScanConsumer";
        private static final String SCAN_REQUEST = "enhancedAsyncScanRequest";
        private static final AsyncTable singleton = new AsyncTable();

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable";
        }

        /**
         * Intercepts an asynchronous enhanced get by key.
         *
         * @param table enhanced table
         * @param key item key
         * @return future mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = GET_ITEM_KEY, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object getItem(Object table,
                                     @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.Key")
                                     Object key) {
            return invokeEnhanced(singleton, GET_ITEM_REQUEST, table, DynamoDbOperationNames.GET_ITEM, key);
        }

        /**
         * Intercepts an asynchronous enhanced get request.
         *
         * @param table enhanced table
         * @param request enhanced get request
         * @return future mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = GET_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object getItem_EM_0(Object table,
                                          @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest")
                                          Object request) {
            return invoke(singleton, GET_ITEM_REQUEST, table, DynamoDbOperationNames.GET_ITEM, request);
        }

        /**
         * Intercepts an asynchronous enhanced put using a mapped item.
         *
         * @param table enhanced table
         * @param item mapped item
         * @return future completion
         */
        @Replacement(type = ReplacementType.TRACKER, id = PUT_ITEM_OBJECT, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object putItem(Object table, Object item) {
            return invokeEnhanced(singleton, PUT_ITEM_REQUEST, table, DynamoDbOperationNames.PUT_ITEM, item);
        }

        /**
         * Intercepts an asynchronous enhanced put request.
         *
         * @param table enhanced table
         * @param request enhanced put request
         * @return future completion
         */
        @Replacement(type = ReplacementType.TRACKER, id = PUT_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object putItem_EM_0(Object table,
                                          @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest")
                                          Object request) {
            return invoke(singleton, PUT_ITEM_REQUEST, table, DynamoDbOperationNames.PUT_ITEM, request);
        }

        /**
         * Intercepts an asynchronous enhanced update using a mapped item.
         *
         * @param table enhanced table
         * @param item mapped item
         * @return future updated mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = UPDATE_ITEM_OBJECT, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object updateItem(Object table, Object item) {
            return invokeEnhanced(singleton, UPDATE_ITEM_REQUEST, table, DynamoDbOperationNames.UPDATE_ITEM, item);
        }

        /**
         * Intercepts an asynchronous enhanced update request.
         *
         * @param table enhanced table
         * @param request enhanced update request
         * @return future updated mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = UPDATE_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object updateItem_EM_0(Object table,
                                             @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest")
                                             Object request) {
            return invoke(singleton, UPDATE_ITEM_REQUEST, table, DynamoDbOperationNames.UPDATE_ITEM, request);
        }

        /**
         * Intercepts an asynchronous enhanced delete by key.
         *
         * @param table enhanced table
         * @param key item key
         * @return future deleted mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = DELETE_ITEM_KEY, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object deleteItem(Object table,
                                        @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.Key")
                                        Object key) {
            return invokeEnhanced(singleton, DELETE_ITEM_REQUEST, table, DynamoDbOperationNames.DELETE_ITEM, key);
        }

        /**
         * Intercepts an asynchronous enhanced delete request.
         *
         * @param table enhanced table
         * @param request enhanced delete request
         * @return future deleted mapped item
         */
        @Replacement(type = ReplacementType.TRACKER, id = DELETE_ITEM_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = COMPLETABLE_FUTURE)
        public static Object deleteItem_EM_0(Object table,
                                             @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest")
                                             Object request) {
            return invoke(singleton, DELETE_ITEM_REQUEST, table, DynamoDbOperationNames.DELETE_ITEM, request);
        }

        /**
         * Intercepts an asynchronous enhanced query configured by a consumer.
         *
         * @param table enhanced table
         * @param requestConsumer query request builder consumer
         * @return query publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = QUERY_CONSUMER, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_PUBLISHER)
        public static Object query(Object table, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, QUERY_REQUEST, table, DynamoDbOperationNames.QUERY, requestConsumer);
        }

        /**
         * Intercepts an asynchronous enhanced query request.
         *
         * @param table enhanced table
         * @param request enhanced query request
         * @return query publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = QUERY_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_PUBLISHER)
        public static Object query_EM_0(Object table,
                                        @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest")
                                        Object request) {
            return invoke(singleton, QUERY_REQUEST, table, DynamoDbOperationNames.QUERY, request);
        }

        /**
         * Intercepts an asynchronous enhanced scan configured by a consumer.
         *
         * @param table enhanced table
         * @param requestConsumer scan request builder consumer
         * @return scan publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = SCAN_CONSUMER, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_PUBLISHER)
        public static Object scan(Object table, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, SCAN_REQUEST, table, DynamoDbOperationNames.SCAN, requestConsumer);
        }

        /**
         * Intercepts an asynchronous enhanced scan request.
         *
         * @param table enhanced table
         * @param request enhanced scan request
         * @return scan publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = SCAN_REQUEST, usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = PAGE_PUBLISHER)
        public static Object scan_EM_0(Object table,
                                       @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest")
                                       Object request) {
            return invoke(singleton, SCAN_REQUEST, table, DynamoDbOperationNames.SCAN, request);
        }
    }

    /**
     * Replacements for {@code DynamoDbEnhancedClient} calls.
     */
    public static class SyncClient extends ThirdPartyMethodReplacementClass {

        private static final String BATCH_GET_ITEM_CONSUMER = "enhancedSyncBatchGetItemConsumer";
        private static final String BATCH_GET_ITEM_REQUEST = "enhancedSyncBatchGetItemRequest";
        private static final SyncClient singleton = new SyncClient();

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient";
        }

        /**
         * Intercepts a synchronous enhanced batch get configured by a consumer.
         *
         * @param client enhanced client
         * @param requestConsumer batch-get request builder consumer
         * @return batch-get pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = BATCH_GET_ITEM_CONSUMER,
                usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB,
                castTo = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable")
        public static Object batchGetItem(Object client, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, BATCH_GET_ITEM_REQUEST, client,
                    DynamoDbOperationNames.BATCH_GET_ITEM, requestConsumer);
        }

        /**
         * Intercepts a synchronous enhanced batch-get request.
         *
         * @param client enhanced client
         * @param request enhanced batch-get request
         * @return batch-get pages
         */
        @Replacement(type = ReplacementType.TRACKER, id = BATCH_GET_ITEM_REQUEST,
                usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB,
                castTo = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable")
        public static Object batchGetItem_EM_0(Object client,
                                               @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest")
                                               Object request) {
            return invoke(singleton, BATCH_GET_ITEM_REQUEST, client,
                    DynamoDbOperationNames.BATCH_GET_ITEM, request);
        }
    }

    /**
     * Replacements for {@code DynamoDbEnhancedAsyncClient} calls.
     */
    public static class AsyncClient extends ThirdPartyMethodReplacementClass {

        private static final String BATCH_GET_ITEM_CONSUMER = "enhancedAsyncBatchGetItemConsumer";
        private static final String BATCH_GET_ITEM_REQUEST = "enhancedAsyncBatchGetItemRequest";
        private static final AsyncClient singleton = new AsyncClient();

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient";
        }

        /**
         * Intercepts an asynchronous enhanced batch get configured by a consumer.
         *
         * @param client enhanced client
         * @param requestConsumer batch-get request builder consumer
         * @return batch-get publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = BATCH_GET_ITEM_CONSUMER,
                usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB,
                castTo = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher")
        public static Object batchGetItem(Object client, Consumer<?> requestConsumer) {
            return invokeEnhanced(singleton, BATCH_GET_ITEM_REQUEST, client,
                    DynamoDbOperationNames.BATCH_GET_ITEM, requestConsumer);
        }

        /**
         * Intercepts an asynchronous enhanced batch-get request.
         *
         * @param client enhanced client
         * @param request enhanced batch-get request
         * @return batch-get publisher
         */
        @Replacement(type = ReplacementType.TRACKER, id = BATCH_GET_ITEM_REQUEST,
                usageFilter = UsageFilter.ANY, category = ReplacementCategory.DYNAMODB,
                castTo = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher")
        public static Object batchGetItem_EM_0(Object client,
                                               @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest")
                                               Object request) {
            return invoke(singleton, BATCH_GET_ITEM_REQUEST, client,
                    DynamoDbOperationNames.BATCH_GET_ITEM, request);
        }
    }

    /**
     * Materializes a convenience-overload argument and invokes the canonical enhanced request overload.
     *
     * @param replacement replacement class owning the method id
     * @param id canonical replacement method id
     * @param receiver enhanced-client receiver
     * @param operationName DynamoDB operation
     * @param argument convenience-overload argument
     * @return original invocation result
     */
    private static Object invokeEnhanced(ThirdPartyMethodReplacementClass replacement, String id,
                                         Object receiver, DynamoDbOperationNames operationName, Object argument) {
        Object enhancedRequest = DynamoDbEnhancedClientRequestConverter
                .toEnhancedRequest(receiver, operationName, argument);
        return invoke(replacement, id, receiver, operationName, enhancedRequest);
    }

    /**
     * Invokes a canonical enhanced-client request overload and records its low-level SDK request.
     *
     * @param replacement replacement class owning the method id
     * @param id replacement method id
     * @param receiver enhanced-client receiver
     * @param operationName DynamoDB operation
     * @param enhancedRequest canonical enhanced request
     * @return original invocation result
     */
    private static Object invoke(ThirdPartyMethodReplacementClass replacement, String id,
                                 Object receiver, DynamoDbOperationNames operationName, Object enhancedRequest) {
        long start = System.currentTimeMillis();
        Object lowLevelRequest = enhancedRequest;
        try {
            lowLevelRequest = DynamoDbEnhancedClientRequestConverter
                    .toLowLevelRequest(receiver, operationName, enhancedRequest);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Preserve application behavior when an enhanced SDK version cannot be converted.
        }
        final Object requestToRecord = lowLevelRequest;
        try {
            Method method = getOriginal(replacement, id, receiver);
            Object result = method.invoke(receiver, enhancedRequest);
            if (result instanceof CompletableFuture) {
                ((CompletableFuture<?>) result).whenComplete((value, throwable) ->
                        record(receiver, operationName, requestToRecord, throwable == null, start));
            } else {
                record(receiver, operationName, requestToRecord, true, start);
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            record(receiver, operationName, requestToRecord, false, start);
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * Records an intercepted enhanced-client invocation in the execution tracer.
     *
     * @param receiver enhanced-client receiver
     * @param operationName DynamoDB operation
     * @param request enhanced-client argument
     * @param successful whether the invocation completed successfully
     * @param start invocation start time
     */
    private static void record(Object receiver, DynamoDbOperationNames operationName, Object request,
                               boolean successful, long start) {
        List<String> tableNames = DynamoDbEnhancedClientRequestConverter.extractTableNames(receiver, request);
        long executionTime = System.currentTimeMillis() - start;
        ExecutionTracer.addDynamoDbInfo(
                new DynamoDbCommand(tableNames, operationName, request, successful, executionTime));
    }
}
