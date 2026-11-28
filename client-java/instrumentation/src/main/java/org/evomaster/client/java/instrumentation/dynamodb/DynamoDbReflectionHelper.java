package org.evomaster.client.java.instrumentation.dynamodb;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Shared reflection helpers used by DynamoDB parser utilities and captured requests.
 */
public final class DynamoDbReflectionHelper {

    private static final String DEBUG_NO_SUCH_METHOD_MESSAGE = "Method '%s' not found on %s";
    private static final String WARN_INVOKE_METHOD_MESSAGE = "Failed to invoke DynamoDB method '%s' on %s: %s";
    private static final String NULL_TARGET = "null";

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
        try {
            return invokeNoArgPropagatingRuntimeExceptions(target, methodName);
        } catch (Exception e) {
            SimpleLogger.uniqueWarn(String.format(WARN_INVOKE_METHOD_MESSAGE, methodName, target == null ? NULL_TARGET : target.getClass().getName(), e));
            return null;
        }
    }

    /**
     * Invokes a no-argument method while allowing runtime reflection failures to propagate.
     *
     * @param target target object
     * @param methodName method name to invoke
     * @return invocation result or {@code null} when the target, method, or invocation is unavailable
     */
    public static Object invokeNoArgPropagatingRuntimeExceptions(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            return method.invoke(target);
        } catch (NoSuchMethodException e) {
            SimpleLogger.debug(String.format(DEBUG_NO_SUCH_METHOD_MESSAGE, methodName, target.getClass().getName()));
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            SimpleLogger.uniqueWarn(
                    String.format(WARN_INVOKE_METHOD_MESSAGE, methodName, target.getClass().getName(), cause));
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
