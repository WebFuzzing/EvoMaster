package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisCommand;
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
import java.util.Optional;

/**
 * This replacement captures operations when Redis is integrated using a CrudRepository.
 * For other libraries such as Lettuce other replacements were included.
 * RedisCommandsClassReplacement and RedisAsyncCommandsClassReplacement are examples of this.
 */
public class RedisKeyValueTemplateClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final RedisKeyValueTemplateClassReplacement singleton = new RedisKeyValueTemplateClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.redis.core.RedisKeyValueTemplate";
    }

    private static final String FIND_BY_ID = "findById";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_BY_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static <T> Optional<T> findById(Object redisKeyValueTemplate, Object id, Class<T> entityType) {
        try {
            long start = System.currentTimeMillis();
            Method findByIdMethod = getOriginal(singleton, FIND_BY_ID, redisKeyValueTemplate);
            Object result = findByIdMethod.invoke(redisKeyValueTemplate, id, entityType);
            long end = System.currentTimeMillis();
            addRedisCommand(id.toString(), end - start);
            return (Optional<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static final String FIND_ALL = "findAll";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = FIND_ALL,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS)
    public static <T> Iterable<T> findAll(Object redisKeyValueTemplate, Class<T> entityType) {
        try {
            long start = System.currentTimeMillis();
            Method findAllMethod = getOriginal(singleton, FIND_ALL, redisKeyValueTemplate);
            Object result = findAllMethod.invoke(redisKeyValueTemplate, entityType);
            long end = System.currentTimeMillis();

            addRedisCommand("ALL:" + entityType.getSimpleName(), end - start);
            return (Iterable<T>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static <T> void addRedisCommand(String keyName, long executionTime) {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.HGETALL,
                keyName,
                null,
                true,
                executionTime
        );
        ExecutionTracer.addRedisCommand(cmd);
    }

}
