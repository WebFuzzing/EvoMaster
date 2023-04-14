package org.evomaster.client.java.controller.mongo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class BsonHelper {
    public static Object newDocument() {
        try {
            return documentClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendToDocument(Object document, String fieldName, Object value) {
        try {
            Method append = documentClass().getMethod("append", String.class, Object.class);
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

    public static Boolean documentContainsField(Object document, String field) {
        try {
            return (Boolean) document.getClass().getMethod("containsKey", Object.class).invoke(document, field);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> documentKeys(Object document) {
        try {
            Class<?> documentClass = Class.forName("org.bson.Document");
            return (Set<String>) documentClass.getMethod("keySet").invoke(document);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean isDocument(Object value) {
        Class<?> documentClass;
        try {
            documentClass = Class.forName("org.bson.Document");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return documentClass.isInstance(value);
    }

    public static Object convertToQueryDocument(Object query) {
        try {
            Method toBsonDocument = query.getClass().getMethod("toBsonDocument");
            Object bsonDocument = toBsonDocument.invoke(query);

            Object documentCodec = documentCodecClass().getDeclaredConstructor().newInstance();
            Method asBsonReader = bsonDocumentClass().getMethod("asBsonReader");
            Method builder = decoderContextClass().getMethod("builder");
            Object builderInstance = builder.invoke(null);
            Method build = builderInstance.getClass().getMethod("build");

            Method decode = documentCodecClass().getMethod("decode", bsonReaderClass(), decoderContextClass());
            return decode.invoke(documentCodec, asBsonReader.invoke(bsonDocument), build.invoke(builderInstance));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getType(Object bsonType) {
        try {
            Class<?> bsonTypeClassMapClass = Class.forName("org.bson.codecs.BsonTypeClassMap");
            Class<?> bsonTypeClass = Class.forName("org.bson.BsonType");
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

    private static Class<?> documentClass() {return getClass("org.bson.Document");}

    private static Class<?> bsonDocumentClass() {return getClass("org.bson.BsonDocument");}

    private static Class<?> documentCodecClass() {return getClass("org.bson.codecs.DocumentCodec");}

    private static Class<?> decoderContextClass() {return getClass("org.bson.codecs.DecoderContext");}

    private static Class<?> bsonReaderClass() {return getClass("org.bson.BsonReader");}

    private static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
