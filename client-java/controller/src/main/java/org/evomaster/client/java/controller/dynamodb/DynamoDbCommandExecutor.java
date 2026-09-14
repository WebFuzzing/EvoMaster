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
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Executes DynamoDB insertions without binding the controller API to an AWS SDK version.
 */
public final class DynamoDbCommandExecutor {

    private static final String ATTRIBUTE_VALUE_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.AttributeValue";
    private static final String ATTRIBUTE_VALUE_BUILDER_CLASS_NAME = ATTRIBUTE_VALUE_CLASS_NAME + "$Builder";
    private static final String PUT_ITEM_REQUEST_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.PutItemRequest";
    private static final String PUT_ITEM_REQUEST_BUILDER_CLASS_NAME = PUT_ITEM_REQUEST_CLASS_NAME + "$Builder";
    private static final String DYNAMODB_CLIENT_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.DynamoDbClient";
    private static final String DYNAMODB_ASYNC_CLIENT_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient";

    private static final String BUILDER_METHOD_NAME = "builder";
    private static final String BUILD_METHOD_NAME = "build";
    private static final String TABLE_NAME_METHOD_NAME = "tableName";
    private static final String ITEM_METHOD_NAME = "item";
    private static final String PUT_ITEM_METHOD_NAME = "putItem";
    private static final String STRING_VALUE_METHOD_NAME = "s";
    private static final String NUMBER_VALUE_METHOD_NAME = "n";
    private static final String BOOLEAN_VALUE_METHOD_NAME = "bool";

    /**
     * Prevents instantiation of this utility class.
     */
    private DynamoDbCommandExecutor() {
    }

    /**
     * Executes insertions using a synchronous or asynchronous AWS SDK v2 client.
     *
     * @param client DynamoDB client
     * @param insertions items to insert
     * @return per-insertion results, stopping at the first failed insertion
     * @throws NullPointerException when the client or insertion list is {@code null}
     * @throws IllegalArgumentException when the insertion list is empty
     */
    public static DynamoDbInsertionResultsDto executeInsert(Object client, List<DynamoDbInsertionDto> insertions) {
        Objects.requireNonNull(client, "DynamoDB client cannot be null");
        Objects.requireNonNull(insertions, "DynamoDB insertions cannot be null");
        if (insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        DynamoDbInsertionResultsDto results = new DynamoDbInsertionResultsDto();
        results.executionResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));
        for (int i = 0; i < insertions.size(); i++) {
            try {
                executeOne(client, insertions.get(i));
                results.executionResults.set(i, true);
            } catch (RuntimeException ignored) {
                results.failedInsertionIndex = i;
                return results;
            }
        }
        return results;
    }

    /**
     * Executes one DynamoDB insertion through the AWS SDK v2 reflection API.
     *
     * @param client synchronous or asynchronous DynamoDB client
     * @param insertion item to insert
     */
    private static void executeOne(Object client, DynamoDbInsertionDto insertion) {
        try {
            ClassLoader loader = client.getClass().getClassLoader();
            Class<?> attributeValueClass = Class.forName(
                    ATTRIBUTE_VALUE_CLASS_NAME, true, loader);
            Class<?> attributeValueBuilderClass = Class.forName(
                    ATTRIBUTE_VALUE_BUILDER_CLASS_NAME, true, loader);
            Class<?> putItemRequestClass = Class.forName(
                    PUT_ITEM_REQUEST_CLASS_NAME, true, loader);
            Class<?> putItemRequestBuilderClass = Class.forName(
                    PUT_ITEM_REQUEST_BUILDER_CLASS_NAME, true, loader);

            Map<String, Object> item = new LinkedHashMap<>();
            for (DynamoDbAttributeValueDto attribute : insertion.attributes) {
                Object builder = attributeValueClass.getMethod(BUILDER_METHOD_NAME).invoke(null);
                String setter;
                Object value = attribute.value;
                switch (attribute.type) {
                    case STRING:
                        setter = STRING_VALUE_METHOD_NAME;
                        break;
                    case NUMBER:
                        setter = NUMBER_VALUE_METHOD_NAME;
                        break;
                    case BOOLEAN:
                        setter = BOOLEAN_VALUE_METHOD_NAME;
                        value = Boolean.valueOf(attribute.value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + attribute.type);
                }
                attributeValueBuilderClass.getMethod(setter, value.getClass()).invoke(builder, value);
                item.put(attribute.attributeName,
                        attributeValueBuilderClass.getMethod(BUILD_METHOD_NAME).invoke(builder));
            }

            Object requestBuilder = putItemRequestClass.getMethod(BUILDER_METHOD_NAME).invoke(null);
            putItemRequestBuilderClass.getMethod(TABLE_NAME_METHOD_NAME, String.class)
                    .invoke(requestBuilder, insertion.tableName);
            putItemRequestBuilderClass.getMethod(ITEM_METHOD_NAME, Map.class).invoke(requestBuilder, item);
            Object request = putItemRequestBuilderClass.getMethod(BUILD_METHOD_NAME).invoke(requestBuilder);
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

    /**
     * Finds the insertion method exposed by a synchronous or asynchronous AWS SDK v2 client.
     *
     * @param client DynamoDB client
     * @param loader client class loader
     * @param putItemRequestClass reflected request class
     * @return reflected {@code putItem} method
     * @throws ClassNotFoundException when the AWS client types are unavailable
     * @throws NoSuchMethodException when the client does not expose the expected method
     */
    private static Method findPutItemMethod(Object client, ClassLoader loader, Class<?> putItemRequestClass)
            throws ClassNotFoundException, NoSuchMethodException {
        Class<?> syncClientClass = Class.forName(DYNAMODB_CLIENT_CLASS_NAME, true, loader);
        if (syncClientClass.isInstance(client)) {
            return syncClientClass.getMethod(PUT_ITEM_METHOD_NAME, putItemRequestClass);
        }

        Class<?> asyncClientClass = Class.forName(DYNAMODB_ASYNC_CLIENT_CLASS_NAME, true, loader);
        if (asyncClientClass.isInstance(client)) {
            return asyncClientClass.getMethod(PUT_ITEM_METHOD_NAME, putItemRequestClass);
        }

        throw new IllegalArgumentException("Unsupported DynamoDB client: " + client.getClass().getName());
    }
}
