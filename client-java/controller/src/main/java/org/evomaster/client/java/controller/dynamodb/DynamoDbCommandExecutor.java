package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Executes DynamoDB insertions without binding the controller API to an AWS SDK version.
 */
public final class DynamoDbCommandExecutor {

    private DynamoDbCommandExecutor() {
    }

    /**
     * Executes insertions using a synchronous or asynchronous AWS SDK v2 client.
     *
     * @param client DynamoDB client
     * @param insertions items to insert
     * @return per-insertion results
     */
    public static DynamoDbInsertionResultsDto executeInsert(Object client, List<DynamoDbInsertionDto> insertions) {
        if (client == null) {
            throw new IllegalArgumentException("No DynamoDB client");
        }
        if (insertions == null || insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        DynamoDbInsertionResultsDto results = new DynamoDbInsertionResultsDto();
        results.executionResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));
        for (int i = 0; i < insertions.size(); i++) {
            try {
                executeOne(client, insertions.get(i));
                results.executionResults.set(i, true);
            } catch (RuntimeException e) {
                results.failedInsertionIndex = i;
                throw new DynamoDbInsertionException(i, results, e);
            }
        }
        return results;
    }

    private static void executeOne(Object client, DynamoDbInsertionDto insertion) {
        try {
            ClassLoader loader = client.getClass().getClassLoader();
            Class<?> attributeValueClass = Class.forName(
                    "software.amazon.awssdk.services.dynamodb.model.AttributeValue", true, loader);
            Class<?> attributeValueBuilderClass = Class.forName(
                    "software.amazon.awssdk.services.dynamodb.model.AttributeValue$Builder", true, loader);
            Class<?> putItemRequestClass = Class.forName(
                    "software.amazon.awssdk.services.dynamodb.model.PutItemRequest", true, loader);
            Class<?> putItemRequestBuilderClass = Class.forName(
                    "software.amazon.awssdk.services.dynamodb.model.PutItemRequest$Builder", true, loader);

            Map<String, Object> item = new LinkedHashMap<>();
            for (DynamoDbAttributeValueDto attribute : insertion.attributes) {
                Object builder = attributeValueClass.getMethod("builder").invoke(null);
                String setter;
                Object value;
                switch (attribute.type) {
                    case S:
                        setter = "s";
                        value = attribute.value;
                        break;
                    case N:
                        setter = "n";
                        value = attribute.value;
                        break;
                    case BOOL:
                        setter = "bool";
                        value = Boolean.valueOf(attribute.value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + attribute.type);
                }
                attributeValueBuilderClass.getMethod(setter, value.getClass()).invoke(builder, value);
                item.put(attribute.attributeName, attributeValueBuilderClass.getMethod("build").invoke(builder));
            }

            Object requestBuilder = putItemRequestClass.getMethod("builder").invoke(null);
            putItemRequestBuilderClass.getMethod("tableName", String.class)
                    .invoke(requestBuilder, insertion.tableName);
            putItemRequestBuilderClass.getMethod("item", Map.class).invoke(requestBuilder, item);
            Object request = putItemRequestBuilderClass.getMethod("build").invoke(requestBuilder);
            Method putItem = findPutItemMethod(client, loader, putItemRequestClass);
            Object response = putItem.invoke(client, request);
            if (response instanceof CompletionStage) {
                ((CompletionStage<?>) response).toCompletableFuture().join();
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed DynamoDB insertion into table '" + insertion.tableName + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed DynamoDB insertion into table '" + insertion.tableName + "'", e);
        }
    }

    private static Method findPutItemMethod(Object client, ClassLoader loader, Class<?> putItemRequestClass)
            throws ClassNotFoundException, NoSuchMethodException {
        Class<?> syncClientClass = Class.forName("software.amazon.awssdk.services.dynamodb.DynamoDbClient", true, loader);
        if (syncClientClass.isInstance(client)) {
            return syncClientClass.getMethod("putItem", putItemRequestClass);
        }

        Class<?> asyncClientClass = Class.forName("software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient", true, loader);
        if (asyncClientClass.isInstance(client)) {
            return asyncClientClass.getMethod("putItem", putItemRequestClass);
        }

        throw new IllegalArgumentException("Unsupported DynamoDB client: " + client.getClass().getName());
    }

    /**
     * Exception carrying partial insertion results.
     */
    public static class DynamoDbInsertionException extends RuntimeException {

        private final int failedIndex;
        private final DynamoDbInsertionResultsDto results;

        private DynamoDbInsertionException(int failedIndex, DynamoDbInsertionResultsDto results, Throwable cause) {
            super("Failed DynamoDB insertion at index " + failedIndex, cause);
            this.failedIndex = failedIndex;
            this.results = results;
        }

        /**
         * @return failed insertion index
         */
        public int getFailedIndex() {
            return failedIndex;
        }

        /**
         * @return partial results
         */
        public DynamoDbInsertionResultsDto getResults() {
            return results;
        }
    }
}
