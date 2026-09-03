package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.dynamodb.DynamoDbAttributeValueHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
            Method scanMethod = findScanMethod(client.getClass());
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
     * Finds an accessible scan method, preferring the public SDK client interface over its
     * package-private implementation class.
     *
     * @param clientClass concrete client class
     * @return accessible scan method
     */
    private Method findScanMethod(Class<?> clientClass) {
        Method interfaceMethod = findScanMethodOnInterfaces(clientClass);
        if (interfaceMethod != null) {
            return interfaceMethod;
        }
        for (Method method : clientClass.getMethods()) {
            if (isScanMethod(method) && Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                return method;
            }
        }
        throw new IllegalArgumentException("The provided client does not expose scan(ScanRequest)");
    }

    /**
     * Searches for public interfaces implemented anywhere in the client's class hierarchy.
     *
     * @param type client class or parent interface
     * @return accessible scan method, or {@code null}
     */
    private Method findScanMethodOnInterfaces(Class<?> type) {
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (Modifier.isPublic(interfaceType.getModifiers())) {
                for (Method method : interfaceType.getMethods()) {
                    if (isScanMethod(method)) {
                        return method;
                    }
                }
            }
            Method inherited = findScanMethodOnInterfaces(interfaceType);
            if (inherited != null) {
                return inherited;
            }
        }
        Class<?> superclass = type.getSuperclass();
        return superclass == null ? null : findScanMethodOnInterfaces(superclass);
    }

    /**
     * Checks whether a reflected method has the AWS SDK scan signature used by this accessor.
     *
     * @param method candidate method
     * @return true when the method accepts one ScanRequest
     */
    private boolean isScanMethod(Method method) {
        return method.getName().equals(SCAN_METHOD)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].getSimpleName().equals(SCAN_REQUEST_CLASS);
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
        Method builderMethod = requestClass.getMethod("builder");
        if (!Modifier.isStatic(builderMethod.getModifiers())) {
            throw new IllegalStateException("DynamoDB ScanRequest.builder() must be static");
        }
        Object builder = builderMethod.invoke(null);
        Class<?> builderClass = builderMethod.getReturnType();
        builderClass.getMethod("tableName", String.class).invoke(builder, tableName);
        if (!startKey.isEmpty()) {
            builderClass.getMethod("exclusiveStartKey", Map.class).invoke(builder, startKey);
        }
        return builderClass.getMethod("build").invoke(builder);
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
        Object rawItems = response.getClass().getMethod("items").invoke(response);
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
        Object rawKey = response.getClass().getMethod("lastEvaluatedKey").invoke(response);
        return rawKey instanceof Map<?, ?> ? (Map<?, ?>) rawKey : Collections.emptyMap();
    }
}
