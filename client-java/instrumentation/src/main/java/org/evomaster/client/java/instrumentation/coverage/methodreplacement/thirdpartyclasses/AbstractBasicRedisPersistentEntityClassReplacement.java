package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisHashSchema;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.RedisTypeToOasConverter;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
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
 * <p>
 * The constructor of {@code BasicRedisPersistentEntity} takes a {@code TypeInformation}, whose package
 * depends on the version of Spring Data Commons: it is {@code org.springframework.data.core} in the latest
 * versions (4.1), and {@code org.springframework.data.util} in the older ones (2.x and 3.x). A replacement of a
 * constructor is matched on its exact signature, so there is one concrete replacement for each of them, and
 * everything they have in common is here:
 * <ul>
 *     <li>{@link BasicRedisPersistentEntityClassReplacement}, for the latest versions</li>
 *     <li>{@link LegacyBasicRedisPersistentEntityClassReplacement}, for the older ones</li>
 * </ul>
 * A version of Spring Data that moves {@code TypeInformation} again needs one more of those.
 * Only one of them can apply to the classes of a given SUT, as only one version of the library is on its
 * classpath.
 */
public abstract class AbstractBasicRedisPersistentEntityClassReplacement extends ThirdPartyMethodReplacementClass {

    protected static final String ENTITY_CLASS_NAME = "org.springframework.data.redis.core.mapping.BasicRedisPersistentEntity";
    protected static final String KEY_SPACE_RESOLVER_CLASS_NAME = "org.springframework.data.keyvalue.core.mapping.KeySpaceResolver";
    protected static final String TIME_TO_LIVE_ACCESSOR_CLASS_NAME = "org.springframework.data.redis.core.TimeToLiveAccessor";
    private static final String ID_ANNOTATION_CLASS_NAME = "org.springframework.data.annotation.Id";
    private static final String GET_KEY_SPACE_METHOD_NAME = "getKeySpace";
    private static final String GET_TYPE_METHOD_NAME = "getType";

    private static final ThreadLocal<Object> instance = new ThreadLocal<>();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return ENTITY_CLASS_NAME;
    }

    /**
     * @return the name of the class that the constructor takes as its first input, which is what tells the
     * versions of Spring Data apart
     */
    protected abstract String getNameOfTypeInformationClass();

    /**
     * A replacement only applies to the version of Spring Data whose {@code TypeInformation} it is for.
     */
    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        try {
            this.getClass().getClassLoader().loadClass(getNameOfTypeInformationClass());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * To be called by the {@code consumeInstance} of each concrete replacement, as the instrumentation looks
     * for that method in the class that declares the replacement of the constructor.
     */
    protected static Object takeInstance() {
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

    /**
     * The entity's id property is the one annotated with Spring Data's {@code @Id} or, failing
     * that, the one named "id" (the same rule Spring applies). It cannot be asked to the persistent
     * entity itself, since its properties are only added after the constructor returns.
     *
     * @return the name of the field, or null if there is none
     */
    private static String findIdFieldName(Class<?> type) {
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
     *
     * @param args                        the inputs of the constructor, in order
     * @param typeInformationClassName    the type of the first input, which depends on the version of Spring Data
     */
    protected static void handleBasicRedisPersistentEntityConstructor(List<Object> args, String typeInformationClassName) {
        try {
            ClassLoader loader = args.get(0).getClass().getClassLoader();
            Class<?> targetClass = loader.loadClass(ENTITY_CLASS_NAME);
            Class<?> informationType = loader.loadClass(typeInformationClassName);
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
