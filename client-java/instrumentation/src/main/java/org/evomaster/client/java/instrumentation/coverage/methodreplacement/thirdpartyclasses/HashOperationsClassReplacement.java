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
public class HashOperationsClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final HashOperationsClassReplacement singleton = new HashOperationsClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.redis.core.HashOperations";
    }

    private static final String GET = "get";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = GET,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static <T> T get(Object hashOps, Object key, Object hashKey) {
        try {
            long start = System.currentTimeMillis();
            Method findOneMethod = getOriginal(singleton, GET, hashOps);
            Object result = findOneMethod.invoke(hashOps, key, hashKey);
            long end = System.currentTimeMillis();

            Class<?> entityClass = result != null ? result.getClass() : Object.class;

            addRedisKeyType(key.toString(), entityClass);
            addRedisInfo(key.toString(), hashKey.toString(), entityClass, result, end - start);
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /*
opsForHash().get(key, field)	HGET	Trae un valor específico de un campo en un hash
opsForHash().entries(key)	HGETALL	Trae todos los campos y valores del hash
opsForHash().values(key)	HVALS	Trae solo los valores del hash
opsForHash().keys(key)	HKEYS	Trae solo las claves del hash

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, category = ReplacementCategory.REDIS)
    public static Map<Object, Object> entries(Object hashOps, Object key) throws Throwable {
        addRedisKeyType(key.toString(), Object.class);  // registrar uso del hash

        Method method = getOriginal(singleton, "entries", hashOps);
        return (Map<Object, Object>) method.invoke(hashOps, key);
    }
     */

    private static <T> void addRedisKeyType(String keyName, Class<T> entityClass) {
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(entityClass, true, Collections.emptyList());
        ExecutionTracer.addRedisSchemaType(new RedisKeySchema(keyName, schema));
    }

    private static <T> void addRedisInfo(String keyName, String hashKey, Class<T> entityClass, Object result, long executionTime) {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGET,
                keyName,
                hashKey,
                result,
                result,
                entityClass,
                true,
                executionTime
        );
        ExecutionTracer.addRedisInfo(cmd);
    }

}
