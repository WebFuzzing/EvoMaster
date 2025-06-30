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
import java.util.Objects;

/**
 * This replacement captures async operations when Redis is integrated using Lettuce.
 * RedisCommandsClassReplacement covers sync operations.
 * For other integrations using CrudRepositories, RedisKeyValueTemplateClassReplacement was included.
 */
public class RedisAsyncCommandsClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final RedisAsyncCommandsClassReplacement singleton = new RedisAsyncCommandsClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.lettuce.core.api.async.RedisAsyncCommands";
    }

    private static final String GET = "get";
    private static final String HGET = "hget";
    private static final String HGETALL = "hgetall";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = GET,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static Object get(Object redis, Object key) {
        try {
            return invokeAndRegisterKeyTypeAndCommand(redis, GET, RedisCommand.RedisCommandType.GET, key, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = HGET,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static Object hget(Object redis, Object key, Object hashKey) {
        try {
            return invokeAndRegisterKeyTypeAndCommand(redis, HGET, RedisCommand.RedisCommandType.HGET, key,
                    hashKey.toString());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = HGETALL,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static Object hgetall(Object redis, Object key) {
        try {
            return invokeAndRegisterKeyTypeAndCommand(redis, HGETALL, RedisCommand.RedisCommandType.HGETALL, key,
                    null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static Object invokeAndRegisterKeyTypeAndCommand(Object redis, String methodId,
                                                             RedisCommand.RedisCommandType commandType, Object key,
                                                             String hashKey)
            throws IllegalAccessException, InvocationTargetException {
        long start = System.currentTimeMillis();
        Method method = getOriginal(singleton, methodId, redis);
        Object result = Objects.isNull(hashKey) ? method.invoke(redis, key) : method.invoke(redis, key, hashKey);
        long end = System.currentTimeMillis();

        Class<?> valueType = !Objects.isNull(result) ? result.getClass() : Object.class;
        addRedisKeyType(key.toString(), valueType);
        addRedisCommand(commandType, key.toString(), hashKey, valueType, end - start);
        return result;
    }

    private static void addRedisKeyType(String keyName, Class<?> valueType) {
        String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(valueType, true, Collections.emptyList());
        ExecutionTracer.addRedisSchemaType(new RedisKeySchema(keyName, schema));
    }

    private static void addRedisCommand(RedisCommand.RedisCommandType type, String key, String hashKey,
                                        Class<?> valueType, long executionTime) {
        RedisCommand cmd = new RedisCommand(
                type,
                key,
                hashKey,
                valueType,
                true,
                executionTime
        );
        ExecutionTracer.addRedisCommand(cmd);
    }
}