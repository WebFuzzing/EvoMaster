package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.internal.db.dynamodb.DynamoDbPrimaryKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

    private static final String DESCRIBE_TABLE_REQUEST_CLASS_SIMPLE = "DescribeTableRequest";

    private static final String SCAN_REQUEST_CLASS_NAME =
            "software.amazon.awssdk.services.dynamodb.model.ScanRequest";
    private static final String SCAN_REQUEST_CLASS_SIMPLE = "ScanRequest";

    private static final String PUT_ITEM_METHOD = "putItem";
    private static final String DESCRIBE_TABLE_METHOD = "describeTable";
    private static final String SCAN_METHOD = "scan";

    private static final String BUILDER_METHOD = "builder";
    private static final String BUILD_METHOD = "build";
    private static final String TABLE_NAME_METHOD = "tableName";
    private static final String ITEM_METHOD = "item";
    private static final String ITEMS_METHOD = "items";
    private static final String TABLE_METHOD = "table";
    private static final String ATTRIBUTE_DEFINITIONS_METHOD = "attributeDefinitions";
    private static final String ATTRIBUTE_NAME_METHOD = "attributeName";
    private static final String ATTRIBUTE_TYPE_METHOD = "attributeType";
    private static final String KEY_SCHEMA_METHOD = "keySchema";
    private static final String EXCLUSIVE_START_KEY_METHOD = "exclusiveStartKey";
    private static final String LAST_EVALUATED_KEY_METHOD = "lastEvaluatedKey";

    private static final String STRING_VALUE_METHOD = "s";
    private static final String NUMBER_VALUE_METHOD = "n";
    private static final String BOOLEAN_VALUE_METHOD = "bool";
    private static final String MAP_VALUE_METHOD = "m";
    private static final String LIST_VALUE_METHOD = "l";
    private static final String NULL_VALUE_METHOD = "nul";

    /** Reflective DynamoDB client instance. */
    private final Object client;

    /** Class loader used to resolve AWS SDK classes. */
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
                item.put(attribute.attributeName,
                        buildAttributeValue(attribute, attributeValueClass, attributeValueBuilderClass));
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
     * Recursively constructs an AWS SDK AttributeValue from a typed DTO.
     *
     * @param attribute typed attribute value DTO
     * @param valueClass reflective AttributeValue class
     * @param builderClass reflective AttributeValue.Builder class
     * @return constructed AttributeValue instance
     * @throws ReflectiveOperationException if reflective invocation fails
     */
    private Object buildAttributeValue(DynamoDbAttributeValueDto attribute, Class<?> valueClass,
                                       Class<?> builderClass) throws ReflectiveOperationException {
        Object builder = valueClass.getMethod(BUILDER_METHOD).invoke(null);
        switch (attribute.type) {
            case MAP:
                Map<String, Object> map = new LinkedHashMap<>();
                for (Map.Entry<String, DynamoDbAttributeValueDto> field : attribute.mapValue.entrySet()) {
                    map.put(field.getKey(), buildAttributeValue(field.getValue(), valueClass, builderClass));
                }
                builderClass.getMethod(MAP_VALUE_METHOD, Map.class).invoke(builder, map);
                break;
            case LIST:
                List<Object> list = new ArrayList<>();
                for (DynamoDbAttributeValueDto child : attribute.listValue) {
                    list.add(buildAttributeValue(child, valueClass, builderClass));
                }
                builderClass.getMethod(LIST_VALUE_METHOD, Collection.class).invoke(builder, list);
                break;
            case NULL:
                builderClass.getMethod(NULL_VALUE_METHOD, Boolean.class).invoke(builder, true);
                break;
            case STRING:
                builderClass.getMethod(STRING_VALUE_METHOD, String.class).invoke(builder, attribute.value);
                break;
            case NUMBER:
                builderClass.getMethod(NUMBER_VALUE_METHOD, String.class).invoke(builder, attribute.value);
                break;
            case BOOLEAN:
                builderClass.getMethod(BOOLEAN_VALUE_METHOD, Boolean.class).invoke(builder, Boolean.valueOf(attribute.value));
                break;
            default:
                throw new IllegalArgumentException("Unsupported DynamoDB attribute type: " + attribute.type);
        }
        return builderClass.getMethod(BUILD_METHOD).invoke(builder);
    }

    /**
     * Reads the base table's primary-key attributes and their DynamoDB scalar types.
     * Secondary-index keys alone are not sufficient for a PutItem request.
     *
     * @param tableName table whose primary key is needed
     * @return immutable primary-key definition in schema order
     */
    public DynamoDbPrimaryKey getPrimaryKeyAttributes(String tableName) {
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

            Map<String, DynamoDbKeyScalarTypeDto> definedTypes = new LinkedHashMap<>();
            for (Object definition : (Collection<?>) table.getClass().getMethod(ATTRIBUTE_DEFINITIONS_METHOD).invoke(table)) {
                String name = (String) definition.getClass().getMethod(ATTRIBUTE_NAME_METHOD).invoke(definition);
                String type = definition.getClass().getMethod(ATTRIBUTE_TYPE_METHOD).invoke(definition).toString();
                if ("S".equals(type)) {
                    definedTypes.put(name, DynamoDbKeyScalarTypeDto.STRING);
                } else if ("N".equals(type)) {
                    definedTypes.put(name, DynamoDbKeyScalarTypeDto.NUMBER);
                } else if ("BOOL".equals(type)) {
                    definedTypes.put(name, DynamoDbKeyScalarTypeDto.BOOLEAN);
                }
            }

            Map<String, DynamoDbKeyScalarTypeDto> keys = new LinkedHashMap<>();
            for (Object key : (Collection<?>) table.getClass().getMethod(KEY_SCHEMA_METHOD).invoke(table)) {
                String name = (String) key.getClass().getMethod(ATTRIBUTE_NAME_METHOD).invoke(key);
                DynamoDbKeyScalarTypeDto type = definedTypes.get(name);
                keys.put(name, type);
            }
            return new DynamoDbPrimaryKey(keys);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed to describe DynamoDB table " + tableName, cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to describe DynamoDB table " + tableName, e);
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

        List<Map<String, Object>> items = new ArrayList<>();
        Map<?, ?> lastEvaluatedKey = Collections.emptyMap();

        try {
            Class<?> scanRequestClass = resolveClass(SCAN_REQUEST_CLASS_NAME);
            Class<?> scanRequestBuilderClass = resolveClass(SCAN_REQUEST_CLASS_NAME + "$Builder");
            Method scanMethod = findClientMethod(SCAN_METHOD, SCAN_REQUEST_CLASS_SIMPLE);

            do {
                Object request = buildScanRequest(scanRequestClass, scanRequestBuilderClass, tableName, lastEvaluatedKey);
                Object response = awaitIfNeeded(scanMethod.invoke(client, request));
                addItems(response, items);
                Map<?, ?> nextKey = readLastEvaluatedKey(response);
                if (!nextKey.isEmpty() && nextKey.equals(lastEvaluatedKey)) {
                    throw new IllegalStateException("DynamoDB scan returned the same pagination key twice");
                }
                lastEvaluatedKey = nextKey;
            } while (!lastEvaluatedKey.isEmpty());

            return items;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reflectively constructs a ScanRequest with optional pagination key.
     *
     * @param requestClass reflective ScanRequest class
     * @param builderClass reflective ScanRequest.Builder class
     * @param tableName target table name
     * @param startKey pagination start key, or empty map if first page
     * @return constructed ScanRequest instance
     * @throws ReflectiveOperationException if reflective construction fails
     */
    private Object buildScanRequest(Class<?> requestClass, Class<?> builderClass, String tableName, Map<?, ?> startKey)
            throws ReflectiveOperationException {
        Method builderMethod = requestClass.getMethod(BUILDER_METHOD);
        Object builder = builderMethod.invoke(null);
        builderClass.getMethod(TABLE_NAME_METHOD, String.class).invoke(builder, tableName);
        if (!startKey.isEmpty()) {
            builderClass.getMethod(EXCLUSIVE_START_KEY_METHOD, Map.class).invoke(builder, startKey);
        }
        return builderClass.getMethod(BUILD_METHOD).invoke(builder);
    }

    /**
     * Normalizes and adds all items from a scan response page into the destination list.
     *
     * @param response scan response object
     * @param destination destination list for normalized item maps
     * @throws ReflectiveOperationException if reading items fails
     */
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

    /**
     * Converts a raw SDK AttributeValue into a normalized Java object (String, Number, Boolean, Map, List, or null).
     *
     * @param attributeValue SDK AttributeValue instance
     * @return normalized Java object
     * @throws ReflectiveOperationException if accessing AttributeValue getters fails
     */
    private Object normalizeAttribute(Object attributeValue) throws ReflectiveOperationException {
        if (attributeValue == null) {
            return null;
        }
        Class<?> valueClass = attributeValue.getClass();

        Object s = valueClass.getMethod(STRING_VALUE_METHOD).invoke(attributeValue);
        if (s != null) {
            return s;
        }
        Object n = valueClass.getMethod(NUMBER_VALUE_METHOD).invoke(attributeValue);
        if (n != null) {
            return n;
        }
        Object bool = valueClass.getMethod(BOOLEAN_VALUE_METHOD).invoke(attributeValue);
        if (bool != null) {
            return bool;
        }
        Object nul = valueClass.getMethod(NULL_VALUE_METHOD).invoke(attributeValue);
        if (Boolean.TRUE.equals(nul)) {
            return null;
        }
        Object m = valueClass.getMethod(MAP_VALUE_METHOD).invoke(attributeValue);
        if (m instanceof Map<?, ?>) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) m).entrySet()) {
                map.put(String.valueOf(entry.getKey()), normalizeAttribute(entry.getValue()));
            }
            return map;
        }
        Object l = valueClass.getMethod(LIST_VALUE_METHOD).invoke(attributeValue);
        if (l instanceof Collection<?>) {
            List<Object> list = new ArrayList<>();
            for (Object child : (Collection<?>) l) {
                list.add(normalizeAttribute(child));
            }
            return list;
        }
        return null;
    }

    /**
     * Reads the last evaluated key from a scan response.
     *
     * @param response scan response object
     * @return map representing last evaluated key, or empty map if none
     * @throws ReflectiveOperationException if method lookup or invocation fails
     */
    private Map<?, ?> readLastEvaluatedKey(Object response) throws ReflectiveOperationException {
        Object rawKey = response.getClass().getMethod(LAST_EVALUATED_KEY_METHOD).invoke(response);
        return rawKey instanceof Map<?, ?> ? (Map<?, ?>) rawKey : Collections.emptyMap();
    }

    /**
     * Waits for an asynchronous completion stage if needed, or returns the synchronous response directly.
     *
     * @param response response object or CompletionStage
     * @return unwrapped response object
     */
    private Object awaitIfNeeded(Object response) {
        if (response instanceof CompletionStage<?>) {
            return ((CompletionStage<?>) response).toCompletableFuture().join();
        }
        return response;
    }

    /**
     * Resolves a class by name using the client classloader.
     *
     * @param className fully-qualified class name
     * @return resolved Class instance
     * @throws ClassNotFoundException if the class cannot be found
     */
    private Class<?> resolveClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, loader);
    }

    /**
     * Finds a matching method on the client instance for the given method name and request class simple name.
     *
     * @param methodName name of the method to invoke
     * @param requestClassName simple name of the request parameter class
     * @return matching Method instance
     * @throws IllegalArgumentException if no matching method is found
     */
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

    /**
     * Searches interface hierarchies for a matching client method.
     *
     * @param type class or interface to inspect
     * @param methodName name of the method
     * @param requestClassName simple name of the request parameter class
     * @return matching Method instance or null if not found
     */
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

    /**
     * Tests whether a method matches the expected name and single-parameter request type.
     *
     * @param method candidate method
     * @param methodName expected method name
     * @param requestClassName expected parameter simple class name
     * @return {@code true} if matching
     */
    private boolean isClientMethod(Method method, String methodName, String requestClassName) {
        return method.getName().equals(methodName)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].getSimpleName().equals(requestClassName);
    }
}
