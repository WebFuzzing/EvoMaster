package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbAttributeType;

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
 * Reads DynamoDB table items without binding controller production code to one AWS SDK version.
 */
final class DynamoDbTableDataAccessor {

    private static final String SCAN_METHOD = "scan";
    private static final String SCAN_REQUEST_CLASS = "ScanRequest";
    private static final String DESCRIBE_TABLE_METHOD = "describeTable";
    private static final String DESCRIBE_TABLE_REQUEST_CLASS = "DescribeTableRequest";
    private static final String BUILDER_METHOD = "builder";
    private static final String BUILD_METHOD = "build";
    private static final String TABLE_NAME_METHOD = "tableName";
    private static final String EXCLUSIVE_START_KEY_METHOD = "exclusiveStartKey";
    private static final String TABLE_METHOD = "table";
    private static final String ATTRIBUTE_DEFINITIONS_METHOD = "attributeDefinitions";
    private static final String ATTRIBUTE_NAME_METHOD = "attributeName";
    private static final String ATTRIBUTE_TYPE_METHOD = "attributeType";
    private static final String KEY_SCHEMA_METHOD = "keySchema";
    private static final String ITEMS_METHOD = "items";
    private static final String LAST_EVALUATED_KEY_METHOD = "lastEvaluatedKey";

    /**
     * Retrieves and normalizes every page of a DynamoDB table scan.
     *
     * @param client AWS SDK v2 synchronous or asynchronous DynamoDB client
     * @param tableName table to scan
     * @return normalized items from all response pages
     */
    List<Map<String, Object>> getItems(Object client, String tableName) {
        Objects.requireNonNull(client, "DynamoDB client cannot be null");
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");

        try {
            Method scanMethod = findClientMethod(client.getClass(), SCAN_METHOD, SCAN_REQUEST_CLASS);
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
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": "
                    + cause.getMessage(), cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to scan DynamoDB table " + tableName + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * Reads the base table's primary-key attributes and their DynamoDB scalar types.
     * Secondary-index keys alone are not sufficient for a PutItem request.
     *
     * @param client AWS SDK v2 synchronous or asynchronous DynamoDB client
     * @param tableName table whose primary key is needed
     * @return immutable primary-key definition in schema order
     */
    DynamoDbPrimaryKey getPrimaryKeyAttributes(Object client, String tableName) {
        Objects.requireNonNull(client, "DynamoDB client cannot be null");
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");

        try {
            Method describeMethod = findClientMethod(client.getClass(), DESCRIBE_TABLE_METHOD,
                    DESCRIBE_TABLE_REQUEST_CLASS);
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

            Map<String, DynamoDbScalarTypeDto> keys = new LinkedHashMap<>();
            for (Object key : (Collection<?>) table.getClass().getMethod(KEY_SCHEMA_METHOD).invoke(table)) {
                String name = (String) key.getClass().getMethod(ATTRIBUTE_NAME_METHOD).invoke(key);
                DynamoDbScalarTypeDto type = definedTypes.get(name);
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
     * Finds a client method through a public interface when the SDK implementation is package-private.
     *
     * @param clientClass concrete client class
     * @param methodName method to find
     * @param requestClassName request parameter's simple class name
     * @return accessible client method
     */
    private Method findClientMethod(Class<?> clientClass, String methodName, String requestClassName) {
        Method interfaceMethod = findMethodOnInterfaces(clientClass, methodName, requestClassName);
        if (interfaceMethod != null) {
            return interfaceMethod;
        }
        for (Method method : clientClass.getMethods()) {
            if (isClientMethod(method, methodName, requestClassName)
                    && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                return method;
            }
        }
        throw new IllegalArgumentException("The provided client does not expose "
                + methodName + "(" + requestClassName + ")");
    }

    /**
     * Searches for public interfaces implemented anywhere in the client's class hierarchy.
     *
     * @param type client class or parent interface
     * @param methodName method to find
     * @param requestClassName request parameter's simple class name
     * @return accessible method, or {@code null}
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
     * Checks whether a reflected method accepts the requested SDK request type.
     *
     * @param method candidate method
     * @param methodName method to find
     * @param requestClassName request parameter's simple class name
     * @return true when the method accepts the request type
     */
    private boolean isClientMethod(Method method, String methodName, String requestClassName) {
        return method.getName().equals(methodName)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].getSimpleName().equals(requestClassName);
    }

    /**
     * Builds one scan request through the SDK's public builder interface.
     *
     * @param requestClass SDK ScanRequest class
     * @param tableName table to scan
     * @param startKey key from the previous page
     * @return scan request
     * @throws ReflectiveOperationException when the SDK request API cannot be invoked
     */
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

    /**
     * Resolves asynchronous SDK responses while leaving synchronous responses unchanged.
     *
     * @param response scan response or completion stage
     * @return resolved scan response
     */
    private Object awaitIfNeeded(Object response) {
        if (response instanceof CompletionStage<?>) {
            return ((CompletionStage<?>) response).toCompletableFuture().join();
        }
        return response;
    }

    /**
     * Normalizes the items in one response page.
     *
     * @param response SDK scan response
     * @param destination normalized item destination
     * @throws ReflectiveOperationException when response items cannot be read
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
     * Reads the pagination key from one response page.
     *
     * @param response SDK scan response
     * @return next pagination key, or an empty map
     * @throws ReflectiveOperationException when the pagination key cannot be read
     */
    private Map<?, ?> readLastEvaluatedKey(Object response) throws ReflectiveOperationException {
        Object rawKey = response.getClass().getMethod(LAST_EVALUATED_KEY_METHOD).invoke(response);
        return rawKey instanceof Map<?, ?> ? (Map<?, ?>) rawKey : Collections.emptyMap();
    }
}
