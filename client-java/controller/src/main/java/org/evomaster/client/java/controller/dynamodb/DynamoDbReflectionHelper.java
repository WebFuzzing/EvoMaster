package org.evomaster.client.java.controller.dynamodb;

import java.lang.reflect.Method;

/**
 * Shared reflection helpers used by DynamoDB parser utilities.
 */
public final class DynamoDbReflectionHelper {

    /**
     * Utility class, no instances.
     */
    private DynamoDbReflectionHelper() {
    }

    /**
     * Invokes a no-argument method on the target object.
     *
     * @param target target object
     * @param methodName method name to invoke
     * @return invocation result or {@code null} on errors
     */
    public static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Invokes a no-argument method and returns it only when boolean.
     *
     * @param target target object
     * @param methodName method name to invoke
     * @return boolean result or {@code null}
     */
    public static Boolean invokeBooleanNoArg(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean ? (Boolean) value : null;
    }
}
