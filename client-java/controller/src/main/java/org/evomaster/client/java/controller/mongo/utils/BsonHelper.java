package org.evomaster.client.java.controller.mongo.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public class BsonHelper {

    private static final String APPEND_METHOD = "append";
    private static final String GET_METHOD = "get";
    private static final String KEY_SET_METHOD = "keySet";
    private static final String CONTAINS_KEY_METHOD = "containsKey";
    private static final String GET_TYPE_NAME_METHOD = "getTypeName";
    private static final String GET_VALUE_METHOD = "getValue";
    private static final String FIND_BY_VALUE_METHOD = "findByValue";
    private static final String GET_PATTERN_METHOD = "getPattern";
    private static final String GET_OPTIONS_METHOD = "getOptions";
    private static final String GET_DATA_METHOD = "getData";
    private static final String TO_BYTE_ARRAY_METHOD = "toByteArray";
    private static final String BIG_DECIMAL_VALUE_METHOD = "bigDecimalValue";

    private static final String ORG_BSON_BSON_BINARY = "org.bson.BsonBinary";
    private static final String ORG_BSON_BSON_TYPE = "org.bson.BsonType";
    private static final String BSON_REGEX_CLASS = "org.bson.BsonRegularExpression";
    private static final String ORG_BSON_DOCUMENT = "org.bson.Document";
    private static final String ORG_BSON_TYPES_BINARY = "org.bson.types.Binary";
    private static final String ORG_BSON_TYPES_DECIMAL_128 = "org.bson.types.Decimal128";
    private static final String ORG_BSON_BSON_UNDEFINED = "org.bson.BsonUndefined";
    private static final String ORG_BSON_TYPES_UNDEFINED = "org.bson.types.Undefined";

    public static final String NULL_TYPE = "null";
    private static final String BSON_TYPE_NULL = "NULL";

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
            // This exception shouldn't go unnoticed.
            throw new RuntimeException(e);
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

    private static final String ORG_BSON_TYPES_OBJECT_ID = "org.bson.types.ObjectId";
    private static final String ORG_BSON_BSON_TIMESTAMP = "org.bson.BsonTimestamp";

    /**
     * Determines whether the given object is a BSON ObjectId.
     *
     * @param obj the object to check; should be non-null to determine if it is a BSON ObjectId
     * @return true if the object is a BSON ObjectId, false otherwise
     */
    public static boolean isObjectId(Object obj) {
        return obj != null && obj.getClass().getName().equals(ORG_BSON_TYPES_OBJECT_ID);
    }

    /**
     * Determines whether the given object is a BSON BsonTimestamp.
     *
     * @param obj the object to check; should be non-null to determine if it is a BSON BsonTimestamp
     * @return true if the object is a BSON BsonTimestamp, false otherwise
     */
    public static boolean isBsonTimestamp(Object obj) {
        return obj != null && obj.getClass().getName().equals(ORG_BSON_BSON_TIMESTAMP);
    }

    /**
     * Determines whether the given object is a BSON type.
     *
     * @param obj the object to check; should be non-null to determine if it is a BSON type
     * @return true if the object is a BSON type, false otherwise
     */
    public static boolean isBsonType(Object obj) {
        return (obj != null) && obj.getClass().getName().equals(ORG_BSON_BSON_TYPE);
    }

    /**
     * Retrieves the value of a BSON BsonTimestamp as a long value.
     *
     * @param bsonTimestamp the BSON BsonTimestamp object; should be non-null
     * @return the value of the BSON BsonTimestamp
     * @throws IllegalArgumentException if the argument is not a BSON BsonTimestamp
     */
    public static long getBsonTimestampValue(Object bsonTimestamp) {
        Objects.requireNonNull(bsonTimestamp);
        if (!isBsonTimestamp(bsonTimestamp)) {
            throw new IllegalArgumentException("argument bsonTimestamp must be a BsonTimestamp");
        }
        try {
            return (Long) bsonTimestamp.getClass().getMethod(GET_VALUE_METHOD).invoke(bsonTimestamp);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    public static String getType(Object bsonType) {
        Objects.requireNonNull(bsonType);
        try {
            ClassLoader bsonTypeClassLoader = bsonType.getClass().getClassLoader();
            Class<?> bsonTypeClass = bsonTypeClassLoader.loadClass(ORG_BSON_BSON_TYPE);
            Object bsonNullTypeInstance = Enum.valueOf(bsonTypeClass.asSubclass(Enum.class), BSON_TYPE_NULL);
            if (bsonType.equals(bsonNullTypeInstance)) {
                return NULL_TYPE;
            } else {
                Class<?> bsonTypeClassMapClass = bsonTypeClassLoader.loadClass("org.bson.codecs.BsonTypeClassMap");
                Object bsonTypeClassMap = bsonTypeClassMapClass.getDeclaredConstructor().newInstance();
                Method get = bsonTypeClassMapClass.getMethod(GET_METHOD, bsonTypeClass);
                Object type = get.invoke(bsonTypeClassMap, bsonType);
                return (String) type.getClass().getMethod(GET_TYPE_NAME_METHOD).invoke(type, null);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Retrieves the BSON type corresponding to the given integer value.
     *
     * @param number the integer value representing the BSON type
     * @return the BSON type object, or null if not found
     */
    public static Object getTypeFromNumber(Integer number) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName(ORG_BSON_BSON_TYPE);
            Method findByValue = bsonTypeClass.getMethod(FIND_BY_VALUE_METHOD, int.class);
            return findByValue.invoke(null, number);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Retrieves the BSON type corresponding to the given alias string.
     *
     * @param name the string representing the BSON type
     * @return the BSON type object, or null if not found
     */
    public static Object bsonTypeValueOf(String name) {
        Class<?> bsonTypeClass;
        try {
            bsonTypeClass = Class.forName(ORG_BSON_BSON_TYPE);
            if (bsonTypeClass.isEnum()) {
                Enum<?> bsonTypeEnum = Enum.valueOf((Class<Enum>) bsonTypeClass, name);
                return bsonTypeEnum;
            } else {
                throw new IllegalArgumentException("BSON type expected to be an enum but is not. Class: " + bsonTypeClass.getName());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the given object is a BSON regular expression.
     *
     * @param object the object to check; should be non-null to determine if it is a BSON regular expression
     * @return true if the object is a BSON regular expression, false otherwise
     */
    public static boolean isBsonRegularExpression(Object object) {
        return object != null && BSON_REGEX_CLASS.equals(object.getClass().getName());
    }

    /**
     * Retrieves the pattern from a BSON regular expression object.
     *
     * @param value the object representing a BSON regular expression. Must not be null and must be a valid BSON regular expression.
     * @return a String representing the pattern of the BSON regular expression.
     * @throws NullPointerException     if the provided value is null.
     * @throws IllegalArgumentException if the provided value is not a BSON regular expression.
     * @throws RuntimeException         if an error occurs while invoking the method to retrieve the pattern.
     */
    public static String bsonRegexGetPattern(Object value) {
        Objects.requireNonNull(value, "The provided value cannot be null");
        if (!isBsonRegularExpression(value)) {
            throw new IllegalArgumentException("The provided value is not a BSON regular expression but class: " + value.getClass().getName());
        }
        try {
            return (String) value.getClass().getMethod(GET_PATTERN_METHOD).invoke(value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the options associated with a BSON regular expression.
     *
     * @param value the object representing a BSON regular expression. Must not be null and must be a valid BSON regular expression.
     * @return a String representing the options associated with the BSON regular expression.
     * @throws NullPointerException     if the provided value is null.
     * @throws IllegalArgumentException if the provided value is not a BSON regular expression.
     **/
    public static String bsonRegexGetOptions(Object value) {
        Objects.requireNonNull(value, "The provided value cannot be null");
        if (!isBsonRegularExpression(value)) {
            throw new IllegalArgumentException("The provided value is not a BSON regular expression but class: " + value.getClass().getName());
        }
        try {
            final Method getOptionsMethod = value.getClass().getMethod(GET_OPTIONS_METHOD);
            return (String) getOptionsMethod.invoke(value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the given object is a BSON binary type.
     *
     * @param value the object to check; should be non-null to determine if it is a BSON binary type
     * @return true if the object is non-null and of BSON binary type, false otherwise
     */
    public static boolean isBsonBinary(Object value) {
        if (value == null) {
            return false;
        }
        String className = value.getClass().getName();
        return className.equals(ORG_BSON_BSON_BINARY) || className.equals(ORG_BSON_TYPES_BINARY);
    }

    public static byte[] getBinaryData(Object value) {
        Objects.requireNonNull(value, "The provided value cannot be null");
        if (!isBsonBinary(value)) {
            throw new IllegalArgumentException("The provided value is not a BSON binary type but class: " + value.getClass().getName());
        }
        try {
            final Method getDataMethod = value.getClass().getMethod(GET_DATA_METHOD);
            return (byte[]) getDataMethod.invoke(value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toByteArray(Object actualValue) {
        Objects.requireNonNull(actualValue, "The provided value cannot be null");
        if (!isObjectId(actualValue)) {
            throw new IllegalArgumentException("The provided value is not a BSON ObjectId but class: " + actualValue.getClass().getName());
        }
        try {
            final Method toByteArrayMethod = actualValue.getClass().getMethod(TO_BYTE_ARRAY_METHOD);
            return (byte[]) toByteArrayMethod.invoke(actualValue);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isDecimal128(Object v) {
        return v!=null && v.getClass().getName().equals(ORG_BSON_TYPES_DECIMAL_128);
    }

    public static BigDecimal getBigDecimalValue(Number n) {
        Objects.requireNonNull(n, "n");

        if (!isDecimal128(n)) {
            throw new IllegalArgumentException("The provided number is not a BSON Decimal128 but class: " + n.getClass().getName());
        }
        try {
            Method bigDecimalValueMethod = n.getClass().getMethod(BIG_DECIMAL_VALUE_METHOD);
            Object decimalValue = bigDecimalValueMethod.invoke(n);
            if (decimalValue instanceof BigDecimal) {
                return (BigDecimal) decimalValue;
            } else {
                throw new IllegalStateException("Expected BigDecimal value but got: " + decimalValue.getClass().getName());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isBsonUndefined(Object v) {
        if (v == null) {
            return false;
        }
        String className = v.getClass().getName();
        return className.equals(ORG_BSON_BSON_UNDEFINED) || className.equals(ORG_BSON_TYPES_UNDEFINED);
    }

    public static boolean isNaN(Object v) {
        Objects.requireNonNull(v);
        if (!isDecimal128(v)) {
            throw new IllegalArgumentException("The provided value is not a BSON Decimal128 but class: " + v.getClass().getName());
        }
        try {
            Method isNaNMethod = v.getClass().getMethod("isNaN");
            boolean result = (boolean) isNaNMethod.invoke(v);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isInfinite(Object v) {
        Objects.requireNonNull(v);
        if (!isDecimal128(v)) {
            throw new IllegalArgumentException("The provided value is not a BSON Decimal128 but class: " + v.getClass().getName());
        }
        try {
            Method isInfiniteMethod = v.getClass().getMethod("isInfinite");
            boolean result = (boolean) isInfiniteMethod.invoke(v);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
