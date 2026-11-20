package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbAttributeType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Talks to the SUT's DynamoDB client through reflection, avoiding compile-time dependency
 * on a specific AWS SDK version.
 * <p>
 * Mirrors the approach of {@code ReflectionBasedRedisClient} and {@code ReflectionBasedNeo4jClient}:
 * every {@code Class.forName}, {@code getMethod}, and {@code invoke} call for DynamoDB operations
 * lives here, behind an ordinary typed API, so callers hold no reflection logic of their own.
 * Supports both synchronous ({@code DynamoDbClient}) and asynchronous ({@code DynamoDbAsyncClient}) clients.
 */
public class ReflectionBasedDynamoDbClient {

    private static final String ATTRIBUTE_VALUE_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.AttributeValue";
    private static final String ATTRIBUTE_VALUE_BUILDER_CLASS_NAME = ATTRIBUTE_VALUE_CLASS_NAME + "$Builder";

    private static final String PUT_ITEM_REQUEST_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.PutItemRequest";
    private static final String PUT_ITEM_REQUEST_BUILDER_CLASS_NAME = PUT_ITEM_REQUEST_CLASS_NAME + "$Builder";
    private static final String PUT_ITEM_REQUEST_CLASS_SIMPLE = "PutItemRequest";

    private static final String SCAN_REQUEST_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.ScanRequest";
    private static final String SCAN_REQUEST_CLASS_SIMPLE = "ScanRequest";
    private static final String DESCRIBE_TABLE_REQUEST_CLASS_SIMPLE = "DescribeTableRequest";

    private static final String PUT_ITEM_METHOD = "putItem";
    private static final String SCAN_METHOD = "scan";
    private static final String DESCRIBE_TABLE_METHOD = "describeTable";

    private static final String BUILDER_METHOD = "builder";
    private static final String BUILD_METHOD = "build";
    private static final String TABLE_NAME_METHOD = "tableName";
    private static final String ITEM_METHOD = "item";
    private static final String ITEMS_METHOD = "items";
    private static final String EXCLUSIVE_START_KEY_METHOD = "exclusiveStartKey";
    private static final String LAST_EVALUATED_KEY_METHOD = "lastEvaluatedKey";
    private static final String TABLE_METHOD = "table";
    private static final String ATTRIBUTE_DEFINITIONS_METHOD = "attributeDefinitions";
    private static final String ATTRIBUTE_NAME_METHOD = "attributeName";
    private static final String ATTRIBUTE_TYPE_METHOD = "attributeType";
    private static final String KEY_SCHEMA_METHOD = "keySchema";
    private static final String KEY_TYPE_METHOD = "keyType";

    private static final String STRING_VALUE_METHOD = "s";
    private static final String NUMBER_VALUE_METHOD = "n";
    private static final String BOOLEAN_VALUE_METHOD = "bool";

    private final Object client;
    private final ClassLoader loader;

    /**
     * Creates a client wrapper for the SUT's DynamoDB client instance.
     *
     * @param client synchronous or asynchronous AWS SDK v2 DynamoDB client
     * @throws NullPointerException when {@code client} is null
     */
    public ReflectionBasedDynamoDbClient(Object client) {
        this.client = Objects.requireNonNull(client, "DynamoDB client cannot be null");
        ClassLoader clientLoader = client.getClass().getClassLoader();
        this.loader = clientLoader != null ? clientLoader : ReflectionBasedDynamoDbClient.class.getClassLoader();
    }

    /**
     * Executes insertions, stopping at the first failed insertion.
     *
     * @param insertions items to insert
     * @return per-insertion execution results
     * @throws NullPointerException when {@code insertions} is null
     * @throws IllegalArgumentException when {@code insertions} is empty
     */
    public DynamoDbInsertionResultsDto executeInsertions(List<DynamoDbInsertionDto> insertions) {
        Objects.requireNonNull(insertions, "DynamoDB insertions cannot be null");
        if (insertions.isEmpty()) {
            throw new IllegalArgumentException("No data to insert");
        }

        DynamoDbInsertionResultsDto results = new DynamoDbInsertionResultsDto();
        results.executionResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));
        for (int i = 0; i < insertions.size(); i++) {
            try {
                executeOne(insertions.get(i));
                results.executionResults.set(i, true);
            } catch (RuntimeException ignored) {
                results.failedInsertionIndex = i;
                return results;
            }
        }
        return results;
    }

    /**
     * Executes a single DynamoDB insertion.
     *
     * @param insertion item to insert
     */
    public void executeOne(DynamoDbInsertionDto insertion) {
        try {
            Class<?> attributeValueClass = resolveClass(ATTRIBUTE_VALUE_CLASS_NAME);
            Class<?> attributeValueBuilderClass = resolveClass(ATTRIBUTE_VALUE_BUILDER_CLASS_NAME);
            Class<?> putItemRequestClass = resolveClass(PUT_ITEM_REQUEST_CLASS_NAME);
            Class<?> putItemRequestBuilderClass = resolveClass(PUT_ITEM_REQUEST_BUILDER_CLASS_NAME);

            Map<String, Object> item = new LinkedHashMap<>();
            for (DynamoDbAttributeValueDto attribute : insertion.attributes) {
                Object builder = attributeValueClass.getMethod(BUILDER_METHOD).invoke(null);
                String setter;
                Object value = attribute.value;
                switch (attribute.type) {
                    case STRING:
                        setter = STRING_VALUE_METHOD;
                        break;
                    case NUMBER:
                        setter = NUMBER_VALUE_METHOD;
                        break;
                    case BOOLEAN:
                        setter = BOOLEAN_VALUE_METHOD;
                        value = Boolean.valueOf(attribute.value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + attribute.type);
                }
                attributeValueBuilderClass.getMethod(setter, value.getClass()).invoke(builder, value);
                item.put(attribute.attributeName,
                        attributeValueBuilderClass.getMethod(BUILD_METHOD).invoke(builder));
            }

            Object requestBuilder = putItemRequestClass.getMethod(BUILDER_METHOD).invoke(null);
            putItemRequestBuilderClass.getMethod(TABLE_NAME_METHOD, String.class)
                    .invoke(requestBuilder, insertion.tableName);
            putItemRequestBuilderClass.getMethod(ITEM_METHOD, Map.class).invoke(requestBuilder, item);
            Object request = putItemRequestBuilderClass.getMethod(BUILD_METHOD).invoke(requestBuilder);

            Method putItem = findClientMethod(PUT_ITEM_METHOD, PUT_ITEM_REQUEST_CLASS_SIMPLE);
            Object response = putItem.invoke(client, request);
            awaitIfNeeded(response);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed DynamoDB insertion into table '" + insertion.tableName + "'", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed DynamoDB insertion into table '" + insertion.tableName + "'", e);
        }
    }

    /**
     * Retrieves and normalizes all items of a table across all scan pages.
     *
     * @param tableName table to scan
     * @return normalized items from all response pages
     */
    public List<Map<String, Object>> getItems(String tableName) {
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");

        try {
            Method scanMethod = findClientMethod(SCAN_METHOD, SCAN_REQUEST_CLASS_SIMPLE);
            Class<?> requestClass = scanMethod.getParameterTypes()[0];
            List<Map<String, Object>> result = new ArrayList<>();
            Map<?, ?> startKey = Collections.emptyMap();

            do {
                Object request = buildScanRequest(requestClass, tableName, startKey);
                Object response = scanMethod.invoke(client, request);
                response = awaitIfNeeded(response);
                addItems(response, result);

                Map<?, ?> nextKey = readLastEvaluatedKey(response);
                if (!nextKey.isEmpty() && nextKey.equals(startKey)) {
                    throw new IllegalStateException("DynamoDB scan returned the same pagination key twice");
                }
                startKey = nextKey;
            } while (!startKey.isEmpty());

            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads the base table's primary-key attributes and scalar types, with the partition key first.
     * Secondary-index keys alone are not sufficient for a PutItem request.
     *
     * @param tableName table whose primary key is needed
     * @return primary-key names and types in schema order
     */
    public Map<String, DynamoDbScalarTypeDto> getPrimaryKeyAttributes(String tableName) {
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");

        try {
            Method describeMethod = findClientMethod(DESCRIBE_TABLE_METHOD, DESCRIBE_TABLE_REQUEST_CLASS_SIMPLE);
            Class<?> requestClass = describeMethod.getParameterTypes()[0];
            Method builderMethod = requestClass.getMethod(BUILDER_METHOD);
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builderMethod.getReturnType();
            builderClass.getMethod(TABLE_NAME_METHOD, String.class).invoke(builder, tableName);
            Object request = builderClass.getMethod(BUILD_METHOD).invoke(builder);
            Object response = awaitIfNeeded(describeMethod.invoke(client, request));
            Object table = response.getClass().getMethod(TABLE_METHOD).invoke(response);

            Map<String, DynamoDbScalarTypeDto> definedTypes = new LinkedHashMap<>();
            for (Object definition : (Collection<?>) table.getClass().getMethod(ATTRIBUTE_DEFINITIONS_METHOD).invoke(table)) {
                String name = (String) definition.getClass().getMethod(ATTRIBUTE_NAME_METHOD).invoke(definition);
                String type = definition.getClass().getMethod(ATTRIBUTE_TYPE_METHOD).invoke(definition).toString();
                if (DynamoDbAttributeType.STRING.getToken().equals(type)) {
                    definedTypes.put(name, DynamoDbScalarTypeDto.STRING);
                } else if (DynamoDbAttributeType.NUMBER.getToken().equals(type)) {
                    definedTypes.put(name, DynamoDbScalarTypeDto.NUMBER);
                }
            }

            Collection<?> keySchema = (Collection<?>) table.getClass().getMethod(KEY_SCHEMA_METHOD).invoke(table);
            Map<String, DynamoDbScalarTypeDto> keys = new LinkedHashMap<>();
            for (String keyType : Arrays.asList("HASH", "RANGE")) {
                for (Object key : keySchema) {
                    if (keyType.equals(key.getClass().getMethod(KEY_TYPE_METHOD).invoke(key).toString())) {
                        String name = (String) key.getClass().getMethod(ATTRIBUTE_NAME_METHOD).invoke(key);
                        keys.put(name, definedTypes.get(name));
                    }
                }
            }
            return keys;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed to describe DynamoDB table " + tableName, cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to describe DynamoDB table " + tableName, e);
        }
    }

    private Object buildScanRequest(Class<?> requestClass, String tableName, Map<?, ?> startKey)
            throws ReflectiveOperationException {
        Method builderMethod = requestClass.getMethod(BUILDER_METHOD);
        if (!Modifier.isStatic(builderMethod.getModifiers())) {
            throw new IllegalStateException("DynamoDB ScanRequest.builder() must be static");
        }
        Object builder = builderMethod.invoke(null);
        Class<?> builderClass = builderMethod.getReturnType();
        builderClass.getMethod(TABLE_NAME_METHOD, String.class).invoke(builder, tableName);
        if (!startKey.isEmpty()) {
            builderClass.getMethod(EXCLUSIVE_START_KEY_METHOD, Map.class).invoke(builder, startKey);
        }
        return builderClass.getMethod(BUILD_METHOD).invoke(builder);
    }

    private void addItems(Object response, List<Map<String, Object>> destination)
            throws ReflectiveOperationException {
        Object rawItems = response.getClass().getMethod(ITEMS_METHOD).invoke(response);
        if (!(rawItems instanceof Collection<?>)) {
            return;
        }
        for (Object rawItem : (Collection<?>) rawItems) {
            Map<String, Object> item = DynamoDbAttributeValueHelper.toPlainMap(rawItem);
            destination.add(item);
        }
    }

    private Map<?, ?> readLastEvaluatedKey(Object response) throws ReflectiveOperationException {
        Object rawKey = response.getClass().getMethod(LAST_EVALUATED_KEY_METHOD).invoke(response);
        return rawKey instanceof Map<?, ?> ? (Map<?, ?>) rawKey : Collections.emptyMap();
    }

    private Object awaitIfNeeded(Object response) {
        if (response instanceof CompletionStage<?>) {
            return ((CompletionStage<?>) response).toCompletableFuture().join();
        }
        return response;
    }

    private Class<?> resolveClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, loader);
    }

    private Method findClientMethod(String methodName, String requestClassName) {
        Method interfaceMethod = findMethodOnInterfaces(client.getClass(), methodName, requestClassName);
        if (interfaceMethod != null) {
            return interfaceMethod;
        }
        for (Method method : client.getClass().getMethods()) {
            if (isClientMethod(method, methodName, requestClassName)
                    && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                return method;
            }
        }
        for (Method method : client.getClass().getMethods()) {
            if (isClientMethod(method, methodName, requestClassName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("The provided client does not expose "
                + methodName + "(" + requestClassName + ")");
    }

    private Method findMethodOnInterfaces(Class<?> type, String methodName, String requestClassName) {
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (Modifier.isPublic(interfaceType.getModifiers())) {
                for (Method method : interfaceType.getMethods()) {
                    if (isClientMethod(method, methodName, requestClassName)) {
                        return method;
                    }
                }
            }
            Method inherited = findMethodOnInterfaces(interfaceType, methodName, requestClassName);
            if (inherited != null) {
                return inherited;
            }
        }
        Class<?> superclass = type.getSuperclass();
        return superclass == null ? null : findMethodOnInterfaces(superclass, methodName, requestClassName);
    }

    private boolean isClientMethod(Method method, String methodName, String requestClassName) {
        return method.getName().equals(methodName)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].getSimpleName().equals(requestClassName);
    }
}
