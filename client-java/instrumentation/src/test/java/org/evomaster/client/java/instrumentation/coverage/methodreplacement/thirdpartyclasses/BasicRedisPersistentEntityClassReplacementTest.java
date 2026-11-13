package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisIdFieldEntities;
import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisMappingContextOperationsImpl;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.RedisHashSchema;
import org.evomaster.client.java.instrumentation.example.redis.RedisMappingContextOperations;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the id field that {@link AbstractBasicRedisPersistentEntityClassReplacement} reports for an entity,
 * going through the replaced constructor: Spring builds the persistent entity inside an
 * {@link InstrumentingClassLoader}, as it does when a repository is bootstrapped in a SUT.
 */
public class BasicRedisPersistentEntityClassReplacementTest {

    private static String defaultReplacement;

    @BeforeAll
    static void setupAll() {
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET,MONGO,REDIS");
    }

    @AfterAll
    static void teardownAll() {
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        } else {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        }
    }

    /**
     * @return the id field of the schema captured for the given entity, or null if the schema has none
     */
    private static String capturedIdField(Class<?> entity) throws Exception {
        ExecutionTracer.reset();

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");
        RedisMappingContextOperations operations = (RedisMappingContextOperations)
                cl.loadClass(RedisMappingContextOperationsImpl.class.getName()).getConstructor().newInstance();
        operations.createPersistentEntity(entity.getName());

        List<RedisHashSchema> schemas = ExecutionTracer.exposeAdditionalInfoList().stream()
                .flatMap(i -> i.getRedisHashSchemaData().stream())
                .collect(Collectors.toList());

        // the keyspace depends on how Spring resolves it under this classloader, so the schema is looked up
        // through the type it was derived from
        List<RedisHashSchema> ofEntity = schemas.stream()
                .filter(s -> s.getHashSchema().contains(entity.getName()))
                .collect(Collectors.toList());

        assertEquals(1, ofEntity.size(), "Expected exactly one schema captured for " + entity.getName());
        return ofEntity.get(0).getIdField();
    }

    @Test
    public void testAnnotatedFieldIsTheId() throws Exception {
        assertEquals("code", capturedIdField(RedisIdFieldEntities.AnnotatedId.class));
    }

    @Test
    public void testFieldNamedIdIsTheIdWhenNothingIsAnnotated() throws Exception {
        assertEquals("id", capturedIdField(RedisIdFieldEntities.NamedId.class));
    }

    @Test
    public void testAnnotationWinsOverTheName() throws Exception {
        assertEquals("key", capturedIdField(RedisIdFieldEntities.BothAnnotatedAndNamed.class));
    }

    @Test
    public void testNoIdIsNull() throws Exception {
        assertNull(capturedIdField(RedisIdFieldEntities.NoId.class));
    }

    @Test
    public void testIdInASuperclassIsFound() throws Exception {
        assertEquals("parentId", capturedIdField(RedisIdFieldEntities.Child.class));
    }

    @Test
    public void testStaticFieldsAreNotTheId() throws Exception {
        assertNull(capturedIdField(RedisIdFieldEntities.StaticIdIsIgnored.class));
    }
}
