package org.evomaster.client.java.controller.mongo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class BsonHelper {
    public static Object newDocument(Object document) {
        try {
            return document.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendToDocument(Object document, String fieldName, Object value) {
        try {
            Method append = document.getClass().getMethod("append", String.class, Object.class);
            append.invoke(document, fieldName, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getValue(Object document, String fieldName) {
        try {
            return document.getClass().getMethod("get", Object.class).invoke(document, fieldName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> keySet(Object document) {
        try {
            return (Set<String>) document.getClass().getMethod("keySet").invoke(document);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean documentContainsField(Object document, String field) {
        try {
            return (Boolean) document.getClass().getMethod("containsKey", Object.class).invoke(document, field);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> documentKeys(Object document) {
        try {
            return (Set<String>) document.getClass().getMethod("keySet").invoke(document);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean isDocument(Object value) {
        return value.getClass().getName().equals("org.bson.Document");
    }

    public static String getType(Object bsonType) {
        try {
            ClassLoader bsonTypeClassLoader = bsonType.getClass().getClassLoader();
            Class<?> bsonTypeClassMapClass = bsonTypeClassLoader.loadClass("org.bson.codecs.BsonTypeClassMap");
            Class<?> bsonTypeClass = bsonTypeClassLoader.loadClass("org.bson.BsonType");
            Object bsonTypeClassMap = bsonTypeClassMapClass.getDeclaredConstructor().newInstance();
            Method get = bsonTypeClassMapClass.getMethod("get", bsonTypeClass);
            Object type = get.invoke(bsonTypeClassMap, bsonType);
            return (String) type.getClass().getMethod("getTypeName").invoke(type, null);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getTypeFromNumber(Integer number) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName("org.bson.BsonType");
            Method findByValue = bsonTypeClass.getMethod("findByValue", int.class);
            return findByValue.invoke(null, number);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getTypeFromAlias(String alias) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName("org.bson.BsonType");
            Method valueOf = bsonTypeClass.getMethod("valueOf", String.class);
            return valueOf.invoke(null, alias);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
