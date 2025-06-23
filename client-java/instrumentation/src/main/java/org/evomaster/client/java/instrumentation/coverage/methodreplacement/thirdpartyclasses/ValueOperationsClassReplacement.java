package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.RedisKeySchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * This replacement captures operations when Redis is integrated using Lettuce or Redisson.
 * For other integrations using CrudRepositories, RedisKeyValueTemplateClassReplacement was included.
 */
public class ValueOperationsClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ValueOperationsClassReplacement singleton = new ValueOperationsClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.redis.core.ValueOperations";
    }

    private static final String GET = "get";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = GET,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static <T> T get(Object valueOps, Object key) {
        try {
            long start = System.currentTimeMillis();
            Method getMethod = getOriginal(singleton, GET, valueOps);
            Object result = getMethod.invoke(valueOps, key);
            long end = System.currentTimeMillis();

            Class<?> entityClass = result != null ? result.getClass() : Object.class;

            addRedisKeyType(key.toString(), entityClass);
            addRedisCommand(key.toString(), entityClass, result, end - start);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static <T> void addRedisKeyType(String keyName, Class<T> entityClass) {
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(entityClass, true, Collections.emptyList());
        ExecutionTracer.addRedisSchemaType(new RedisKeySchema(keyName, schema));
    }

    private static <T> void addRedisCommand(String keyName, Class<T> entityClass, Object result, long executionTime) {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.GET,
                keyName,
                null,
                result,
                result,
                entityClass,
                true,
                executionTime
        );
        ExecutionTracer.addRedisCommand(cmd);
    }

}
