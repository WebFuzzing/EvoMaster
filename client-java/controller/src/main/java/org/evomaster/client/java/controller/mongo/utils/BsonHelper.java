package org.evomaster.client.java.controller.mongo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

public class BsonHelper {

    private static final String APPEND_METHOD = "append";
    private static final String GET_METHOD = "get";
    private static final String KEY_SET_METHOD = "keySet";
    private static final String CONTAINS_KEY_METHOD = "containsKey";
    private static final String GET_TYPE_NAME_METHOD = "getTypeName";
    private static final String FIND_BY_VALUE_METHOD = "findByValue";
    private static final String VALUE_OF_METHOD = "valueOf";

    private static final String ORG_BSON_BSON_TYPE = "org.bson.BsonType";
    private static final String ORG_BSON_DOCUMENT = "org.bson.Document";

    public static Object newDocument(Object bsonDocument) {
        Objects.requireNonNull(bsonDocument);
        if (!isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("argument bsonDocument must be a BsonDocument");
        }
        try {
            return bsonDocument.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendToDocument(Object bsonDocument, String fieldName, Object value) {
        Objects.requireNonNull(bsonDocument);
        Objects.requireNonNull(fieldName);
        if (!isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("argument bsonDocument must be a BsonDocument");
        }
        try {
            Method append = bsonDocument.getClass().getMethod(APPEND_METHOD, String.class, Object.class);
            append.invoke(bsonDocument, fieldName, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getValue(Object bsonDocument, String fieldName) {
        Objects.requireNonNull(bsonDocument);
        Objects.requireNonNull(fieldName);
        if (!isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("argument bsonDocument must be a BsonDocument");
        }
        try {
            return bsonDocument.getClass().getMethod(GET_METHOD, Object.class).invoke(bsonDocument, fieldName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean documentContainsField(Object bsonDocument, String fieldName) {
        Objects.requireNonNull(bsonDocument);
        Objects.requireNonNull(fieldName);
        if (!isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("argument bsonDocument must be a BsonDocument");
        }
        try {
            return (Boolean) bsonDocument.getClass().getMethod(CONTAINS_KEY_METHOD, Object.class).invoke(bsonDocument, fieldName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> documentKeys(Object bsonDocument) {
        Objects.requireNonNull(bsonDocument);
        if (!isBsonDocument(bsonDocument)) {
            throw new IllegalArgumentException("argument bsonDocument must be a BsonDocument");
        }
        try {
            return (Set<String>) bsonDocument.getClass().getMethod(KEY_SET_METHOD).invoke(bsonDocument);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Checks if the given object represents a BSON document.
     *
     * @param value the object to check; should be non-null to determine if it is a BSON document
     * @return true if the object is a BSON document, false otherwise
     */
    public static Boolean isBsonDocument(Object value) {
        return value != null && value.getClass().getName().equals(ORG_BSON_DOCUMENT);
    }

    public static String getType(Object bsonType) {
        try {
            ClassLoader bsonTypeClassLoader = bsonType.getClass().getClassLoader();
            Class<?> bsonTypeClassMapClass = bsonTypeClassLoader.loadClass("org.bson.codecs.BsonTypeClassMap");
            Class<?> bsonTypeClass = bsonTypeClassLoader.loadClass(ORG_BSON_BSON_TYPE);
            Object bsonTypeClassMap = bsonTypeClassMapClass.getDeclaredConstructor().newInstance();
            Method get = bsonTypeClassMapClass.getMethod(GET_METHOD, bsonTypeClass);
            Object type = get.invoke(bsonTypeClassMap, bsonType);
            return (String) type.getClass().getMethod(GET_TYPE_NAME_METHOD).invoke(type, null);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getTypeFromNumber(Integer number) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName(ORG_BSON_BSON_TYPE);
            Method findByValue = bsonTypeClass.getMethod(FIND_BY_VALUE_METHOD, int.class);
            return findByValue.invoke(null, number);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getTypeFromAlias(String alias) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName(ORG_BSON_BSON_TYPE);
            Method valueOf = bsonTypeClass.getMethod(VALUE_OF_METHOD, String.class);
            return valueOf.invoke(null, alias);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
