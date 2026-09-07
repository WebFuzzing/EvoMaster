package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Converts DynamoDB enhanced-client arguments into the low-level SDK requests used by the heuristics parser.
 */
final class DynamoDbEnhancedClientRequestConverter {

    private static final String ENHANCED_PACKAGE = "software.amazon.awssdk.enhanced.dynamodb.";
    private static final String MODEL_PACKAGE = ENHANCED_PACKAGE + "model.";
    private static final String OPERATIONS_PACKAGE = ENHANCED_PACKAGE + "internal.operations.";
    private static final String MAPPED_TABLE_RESOURCE = ENHANCED_PACKAGE + "MappedTableResource";

    private DynamoDbEnhancedClientRequestConverter() {
    }

    /**
     * Builds the canonical enhanced request used by an enhanced-client convenience overload.
     *
     * @param receiver enhanced table or client
     * @param operationName intercepted operation
     * @param argument convenience-overload argument
     * @return canonical enhanced request
     */
    static Object toEnhancedRequest(Object receiver, DynamoDbOperationNames operationName, Object argument) {
        String requestClassName = enhancedRequestClassName(operationName);
        ClassLoader loader = receiver.getClass().getClassLoader();
        try {
            Class<?> requestClass = loader.loadClass(requestClassName);
            if (requestClass.isInstance(argument)) {
                return argument;
            }

            Object builder = createBuilder(receiver, requestClass, operationName);
            if (argument instanceof Consumer) {
                @SuppressWarnings("unchecked")
                Consumer<Object> consumer = (Consumer<Object>) argument;
                consumer.accept(builder);
            } else {
                String property = requestProperty(operationName);
                Class<?> propertyType = "key".equals(property)
                        ? loader.loadClass(ENHANCED_PACKAGE + "Key")
                        : Object.class;
                Class<?> builderClass = loader.loadClass(requestClassName + "$Builder");
                builderClass.getMethod(property, propertyType).invoke(builder, argument);
            }
            return loader.loadClass(requestClassName + "$Builder").getMethod("build").invoke(builder);
        } catch (InvocationTargetException e) {
            throw propagate(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build DynamoDB enhanced request", e);
        }
    }

    /**
     * Converts a canonical enhanced request with the same AWS operation used by the enhanced client.
     *
     * @param receiver enhanced table or client
     * @param operationName intercepted operation
     * @param enhancedRequest canonical enhanced request
     * @return low-level DynamoDB request
     * @throws ReflectiveOperationException when the installed enhanced SDK is incompatible
     */
    static Object toLowLevelRequest(Object receiver, DynamoDbOperationNames operationName, Object enhancedRequest)
            throws ReflectiveOperationException {
        ClassLoader loader = receiver.getClass().getClassLoader();
        Class<?> requestClass = loader.loadClass(enhancedRequestClassName(operationName));
        Class<?> operationClass = loader.loadClass(operationClassName(operationName));
        Object operation = operationClass.getMethod("create", requestClass).invoke(null, enhancedRequest);

        if (operationName == DynamoDbOperationNames.BATCH_GET_ITEM) {
            Class<?> extensionClass = loader.loadClass(ENHANCED_PACKAGE + "DynamoDbEnhancedClientExtension");
            return operationClass.getMethod("generateRequest", extensionClass).invoke(operation, new Object[]{null});
        }

        Class<?> mappedTableClass = loader.loadClass(MAPPED_TABLE_RESOURCE);
        Object tableSchema = mappedTableClass.getMethod("tableSchema").invoke(receiver);
        Object tableName = mappedTableClass.getMethod("tableName").invoke(receiver);
        Object extension = mappedTableClass.getMethod("mapperExtension").invoke(receiver);

        Class<?> tableMetadataClass = loader.loadClass(ENHANCED_PACKAGE + "TableMetadata");
        Object primaryIndex = tableMetadataClass.getMethod("primaryIndexName").invoke(null);
        Class<?> contextClass = loader.loadClass(OPERATIONS_PACKAGE + "DefaultOperationContext");
        Object context = contextClass.getMethod("create", String.class, String.class)
                .invoke(null, tableName, primaryIndex);

        Class<?> tableSchemaClass = loader.loadClass(ENHANCED_PACKAGE + "TableSchema");
        Class<?> operationContextClass = loader.loadClass(ENHANCED_PACKAGE + "OperationContext");
        Class<?> extensionClass = loader.loadClass(ENHANCED_PACKAGE + "DynamoDbEnhancedClientExtension");
        return operationClass.getMethod("generateRequest", tableSchemaClass, operationContextClass, extensionClass)
                .invoke(operation, tableSchema, context, extension);
    }

    /**
     * Extracts affected table names from the receiver or a generated batch request.
     *
     * @param receiver enhanced table or client
     * @param lowLevelRequest generated low-level request
     * @return deterministic table-name list
     */
    static List<String> extractTableNames(Object receiver, Object lowLevelRequest) {
        try {
            ClassLoader loader = receiver.getClass().getClassLoader();
            Class<?> mappedTableClass = loader.loadClass(MAPPED_TABLE_RESOURCE);
            if (mappedTableClass.isInstance(receiver)) {
                Object tableName = mappedTableClass.getMethod("tableName").invoke(receiver);
                return tableName instanceof String
                        ? Collections.singletonList((String) tableName)
                        : Collections.<String>emptyList();
            }

            Method requestItems = lowLevelRequest.getClass().getMethod("requestItems");
            Object value = requestItems.invoke(lowLevelRequest);
            if (value instanceof Map) {
                List<String> tableNames = new ArrayList<>(((Map<String, ?>) value).keySet());
                Collections.sort(tableNames);
                return tableNames;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | ClassCastException e) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * Creates the correct builder, including the mapped item type required by write requests.
     */
    private static Object createBuilder(Object receiver, Class<?> requestClass,
                                        DynamoDbOperationNames operationName)
            throws ReflectiveOperationException {
        if (operationName != DynamoDbOperationNames.PUT_ITEM &&
                operationName != DynamoDbOperationNames.UPDATE_ITEM) {
            return requestClass.getMethod("builder").invoke(null);
        }

        ClassLoader loader = receiver.getClass().getClassLoader();
        Class<?> mappedTableClass = loader.loadClass(MAPPED_TABLE_RESOURCE);
        Object tableSchema = mappedTableClass.getMethod("tableSchema").invoke(receiver);
        Class<?> tableSchemaClass = loader.loadClass(ENHANCED_PACKAGE + "TableSchema");
        Object itemType = tableSchemaClass.getMethod("itemType").invoke(tableSchema);
        Class<?> enhancedTypeClass = loader.loadClass(ENHANCED_PACKAGE + "EnhancedType");
        Object rawClass = enhancedTypeClass.getMethod("rawClass").invoke(itemType);
        return requestClass.getMethod("builder", Class.class).invoke(null, rawClass);
    }

    /**
     * Returns the enhanced request class for an intercepted operation.
     */
    private static String enhancedRequestClassName(DynamoDbOperationNames operationName) {
        switch (operationName) {
            case GET_ITEM:
                return MODEL_PACKAGE + "GetItemEnhancedRequest";
            case PUT_ITEM:
                return MODEL_PACKAGE + "PutItemEnhancedRequest";
            case UPDATE_ITEM:
                return MODEL_PACKAGE + "UpdateItemEnhancedRequest";
            case DELETE_ITEM:
                return MODEL_PACKAGE + "DeleteItemEnhancedRequest";
            case QUERY:
                return MODEL_PACKAGE + "QueryEnhancedRequest";
            case SCAN:
                return MODEL_PACKAGE + "ScanEnhancedRequest";
            case BATCH_GET_ITEM:
                return MODEL_PACKAGE + "BatchGetItemEnhancedRequest";
            default:
                throw new IllegalArgumentException("Unsupported enhanced DynamoDB operation: " + operationName);
        }
    }

    /**
     * Returns the internal AWS operation class for an intercepted operation.
     */
    private static String operationClassName(DynamoDbOperationNames operationName) {
        switch (operationName) {
            case GET_ITEM:
                return OPERATIONS_PACKAGE + "GetItemOperation";
            case PUT_ITEM:
                return OPERATIONS_PACKAGE + "PutItemOperation";
            case UPDATE_ITEM:
                return OPERATIONS_PACKAGE + "UpdateItemOperation";
            case DELETE_ITEM:
                return OPERATIONS_PACKAGE + "DeleteItemOperation";
            case QUERY:
                return OPERATIONS_PACKAGE + "QueryOperation";
            case SCAN:
                return OPERATIONS_PACKAGE + "ScanOperation";
            case BATCH_GET_ITEM:
                return OPERATIONS_PACKAGE + "BatchGetItemOperation";
            default:
                throw new IllegalArgumentException("Unsupported enhanced DynamoDB operation: " + operationName);
        }
    }

    /**
     * Returns the builder property used by a convenience overload.
     */
    private static String requestProperty(DynamoDbOperationNames operationName) {
        switch (operationName) {
            case GET_ITEM:
            case DELETE_ITEM:
                return "key";
            case PUT_ITEM:
            case UPDATE_ITEM:
                return "item";
            default:
                throw new IllegalArgumentException("Operation requires a request consumer: " + operationName);
        }
    }

    /**
     * Preserves the exception thrown by an enhanced request builder.
     */
    private static RuntimeException propagate(Throwable cause) {
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return new IllegalStateException(cause);
    }
}
