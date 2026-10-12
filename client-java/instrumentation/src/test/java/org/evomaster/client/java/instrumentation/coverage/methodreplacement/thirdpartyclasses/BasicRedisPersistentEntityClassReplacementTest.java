package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BasicRedisPersistentEntityClassReplacementTest {

    static class AnnotatedId {
        String name;
        @Id
        String code;
    }

    static class NamedId {
        String name;
        Long id;
    }

    static class BothAnnotatedAndNamed {
        String id;
        @Id
        String key;
    }

    static class NoId {
        String name;
    }

    static class Parent {
        @Id
        String parentId;
    }

    static class Child extends Parent {
        String name;
    }

    static class StaticIdIsIgnored {
        static String id;
        String name;
    }

    @Test
    public void testAnnotatedFieldIsTheId() {
        assertEquals("code", BasicRedisPersistentEntityClassReplacement.findIdFieldName(AnnotatedId.class));
    }

    @Test
    public void testFieldNamedIdIsTheIdWhenNothingIsAnnotated() {
        assertEquals("id", BasicRedisPersistentEntityClassReplacement.findIdFieldName(NamedId.class));
    }

    @Test
    public void testAnnotationWinsOverTheName() {
        assertEquals("key", BasicRedisPersistentEntityClassReplacement.findIdFieldName(BothAnnotatedAndNamed.class));
    }

    @Test
    public void testNoIdIsNull() {
        assertNull(BasicRedisPersistentEntityClassReplacement.findIdFieldName(NoId.class));
    }

    @Test
    public void testIdInASuperclassIsFound() {
        assertEquals("parentId", BasicRedisPersistentEntityClassReplacement.findIdFieldName(Child.class));
    }

    @Test
    public void testStaticFieldsAreNotTheId() {
        assertNull(BasicRedisPersistentEntityClassReplacement.findIdFieldName(StaticIdIsIgnored.class));
    }
}
