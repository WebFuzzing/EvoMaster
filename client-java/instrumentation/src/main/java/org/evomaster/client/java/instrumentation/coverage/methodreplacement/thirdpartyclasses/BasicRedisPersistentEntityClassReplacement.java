package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisHashSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

/**
 * When a Spring Data Redis repository ({@code @RedisHash}) is bootstrapped, Spring builds a
 * {@code BasicRedisPersistentEntity} for the annotated class, once per class. It is the only place
 * that ties together the keyspace name (used as the prefix of every key of that type, e.g.
 * "entities:1") and the Java type of the hash's fields; neither the keyspace nor the connection
 * used afterwards carries that type information. The intention of this replacement, mirroring
 * {@link MappingMongoEntityInformationClassReplacement}, is to capture it, so that a HGETALL/HGET
 * failing against a key of that keyspace can be satisfied with a hash of the right shape instead
 * of a single placeholder field.
 */
public class BasicRedisPersistentEntityClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ThreadLocal<Object> instance = new ThreadLocal<>();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity";
    }

    public static Object consumeInstance() {
        Object entity = instance.get();
        if (entity == null) {
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return entity;
    }

    private static void addInstance(Object x) {
        Object entity = instance.get();
        if (entity != null) {
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.REDIS,
            id = "constructorEntity",
            castTo = "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity"
    )
    public static void BasicRedisPersistentEntity(
            @ThirdPartyCast(actualType = "org.springframework.data.util.TypeInformation") Object information,
            @ThirdPartyCast(actualType = "org.springframework.data.keyvalue.core.mapping.KeySpaceResolver") Object fallbackKeySpaceResolver,
            @ThirdPartyCast(actualType = "org.springframework.data.redis.core.TimeToLiveAccessor") Object timeToLiveAccessor) {
        handleBasicRedisPersistentEntityConstructor(
                Arrays.asList(information, fallbackKeySpaceResolver, timeToLiveAccessor));
    }

    /**
     * Resolves the original constructor and invokes it directly via the classloader of the actual
     * arguments received at the call site, rather than reusing a {@link Constructor} object cached
     * by {@link #getOriginalConstructor(ThirdPartyMethodReplacementClass, String)}: that cache is
     * keyed by {@code UnitsInfoRecorder}'s "first classloader ever seen" for this target class,
     * which can go stale across classloader instances (e.g. across JUnit test methods, each
     * bootstrapping its own {@code InstrumentingClassLoader}), producing a {@link Constructor} that
     * intermittently fails with "argument type mismatch" despite its declaring class and parameter
     * types being identical (by reference) to the ones the arguments were actually loaded with.
     */
    private static void handleBasicRedisPersistentEntityConstructor(List<Object> args) {
        try {
            ClassLoader loader = args.get(0).getClass().getClassLoader();
            Class<?> targetClass = loader.loadClass(
                    "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity");
            Class<?> informationType = loader.loadClass("org.springframework.data.util.TypeInformation");
            Class<?> keySpaceResolverType = loader.loadClass("org.springframework.data.keyvalue.core.mapping.KeySpaceResolver");
            Class<?> timeToLiveAccessorType = loader.loadClass("org.springframework.data.redis.core.TimeToLiveAccessor");

            Constructor constructor = targetClass.getConstructor(informationType, keySpaceResolverType, timeToLiveAccessorType);
            Object entity = constructor.newInstance(args.toArray());
            addInstance(entity);
            String keySpace = (String) entity.getClass().getMethod("getKeySpace").invoke(entity);
            Class<?> type = (Class<?>) entity.getClass().getMethod("getType").invoke(entity);
            String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(type);
            ExecutionTracer.addRedisHashType(new RedisHashSchema(keySpace, schema));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
