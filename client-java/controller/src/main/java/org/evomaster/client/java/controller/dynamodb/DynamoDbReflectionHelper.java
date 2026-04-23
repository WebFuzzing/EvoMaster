package org.evomaster.client.java.controller.dynamodb;

import java.lang.reflect.Method;

/**
 * Shared reflection helpers used by DynamoDB parser utilities.
 */
public final class DynamoDbReflectionHelper {

    private DynamoDbReflectionHelper() {
    }

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

    public static Boolean invokeBooleanNoArg(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean ? (Boolean) value : null;
    }
}
