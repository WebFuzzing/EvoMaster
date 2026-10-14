package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisHashSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.RedisTypeToOasConverter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
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

    private static final String ENTITY_CLASS_NAME = "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity";
    private static final String TYPE_INFORMATION_CLASS_NAME = "org.springframework.data.util.TypeInformation";
    private static final String KEY_SPACE_RESOLVER_CLASS_NAME = "org.springframework.data.keyvalue.core.mapping.KeySpaceResolver";
    private static final String TIME_TO_LIVE_ACCESSOR_CLASS_NAME = "org.springframework.data.redis.core.TimeToLiveAccessor";
    private static final String ID_ANNOTATION_CLASS_NAME = "org.springframework.data.annotation.Id";
    private static final String GET_KEY_SPACE_METHOD_NAME = "getKeySpace";
    private static final String GET_TYPE_METHOD_NAME = "getType";

    private static final ThreadLocal<Object> instance = new ThreadLocal<>();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return ENTITY_CLASS_NAME;
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
            castTo = ENTITY_CLASS_NAME
    )
    public static void BasicRedisPersistentEntity(
            @ThirdPartyCast(actualType = TYPE_INFORMATION_CLASS_NAME) Object information,
            @ThirdPartyCast(actualType = KEY_SPACE_RESOLVER_CLASS_NAME) Object fallbackKeySpaceResolver,
            @ThirdPartyCast(actualType = TIME_TO_LIVE_ACCESSOR_CLASS_NAME) Object timeToLiveAccessor) {
        handleBasicRedisPersistentEntityConstructor(
                Arrays.asList(information, fallbackKeySpaceResolver, timeToLiveAccessor));
    }

    /**
     * The entity's id property is the one annotated with Spring Data's {@code @Id} or, failing
     * that, the one named "id" (the same rule Spring applies). It cannot be asked to the persistent
     * entity itself, since its properties are only added after the constructor returns.
     *
     * Package-private only so that it can be unit tested directly, with plain classes, instead of
     * going through the replaced constructor.
     *
     * @return the name of the field, or null if there is none
     */
    static String findIdFieldName(Class<?> type) {
        String namedId = null;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().getName().equals(ID_ANNOTATION_CLASS_NAME)) {
                        return f.getName();
                    }
                }
                if (namedId == null && f.getName().equals("id")) {
                    namedId = f.getName();
                }
            }
        }
        return namedId;
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
            Class<?> targetClass = loader.loadClass(ENTITY_CLASS_NAME);
            Class<?> informationType = loader.loadClass(TYPE_INFORMATION_CLASS_NAME);
            Class<?> keySpaceResolverType = loader.loadClass(KEY_SPACE_RESOLVER_CLASS_NAME);
            Class<?> timeToLiveAccessorType = loader.loadClass(TIME_TO_LIVE_ACCESSOR_CLASS_NAME);

            Constructor constructor = targetClass.getConstructor(informationType, keySpaceResolverType, timeToLiveAccessorType);
            Object entity = constructor.newInstance(args.toArray());
            addInstance(entity);
            String keySpace = (String) entity.getClass().getMethod(GET_KEY_SPACE_METHOD_NAME).invoke(entity);
            Class<?> type = (Class<?>) entity.getClass().getMethod(GET_TYPE_METHOD_NAME).invoke(entity);
            String schema = ClassToSchema.getOrDeriveSchemaWithItsRef(type, false, RedisTypeToOasConverter.defaults());
            ExecutionTracer.addRedisHashType(new RedisHashSchema(keySpace, schema, findIdFieldName(type)));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
