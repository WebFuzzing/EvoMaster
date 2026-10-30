package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.springframework.data.annotation.Id;

/**
 * Entities that differ only in how their id field is declared, used to check which field name the
 * BasicRedisPersistentEntity replacement reports as the id of the hash.
 */
public class RedisIdFieldEntities {

    public static class AnnotatedId {
        String name;
        @Id
        String code;
    }

    public static class NamedId {
        String name;
        Long id;
    }

    public static class BothAnnotatedAndNamed {
        String id;
        @Id
        String key;
    }

    public static class NoId {
        String name;
    }

    public static class Parent {
        @Id
        String parentId;
    }

    public static class Child extends Parent {
        String name;
    }

    public static class StaticIdIsIgnored {
        static String id;
        String name;
    }
}
