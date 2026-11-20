package org.evomaster.client.java.instrumentation.dynamodb;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Persistent instrumentation metadata, refreshed whenever an enhanced table is mapped. */
public final class DynamoDbSchemaRegistry {
    private static final String CONVERTER_PACKAGE =
            "software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.";
    private static final String OPTIONAL_CONVERTER = CONVERTER_PACKAGE + "OptionalAttributeConverter";
    private static final String INSTANT_AS_STRING_CONVERTER = CONVERTER_PACKAGE + "InstantAsStringAttributeConverter";
    private static final String OFFSET_DATE_TIME_AS_STRING_CONVERTER =
            CONVERTER_PACKAGE + "OffsetDateTimeAsStringAttributeConverter";
    private static final String ZONED_DATE_TIME_AS_STRING_CONVERTER =
            CONVERTER_PACKAGE + "ZonedDateTimeAsStringAttributeConverter";
    private static final String LOCAL_DATE_TIME_CONVERTER = CONVERTER_PACKAGE + "LocalDateTimeAttributeConverter";
    private static final String LOCAL_DATE_CONVERTER = CONVERTER_PACKAGE + "LocalDateAttributeConverter";
    private static final String LOCAL_TIME_CONVERTER = CONVERTER_PACKAGE + "LocalTimeAttributeConverter";
    private static final String UUID_CONVERTER = CONVERTER_PACKAGE + "UuidAttributeConverter";
    private static final String ATOMIC_INTEGER_CONVERTER = CONVERTER_PACKAGE + "AtomicIntegerAttributeConverter";

    private static final String METHOD_ATTRIBUTE_NAMES = "attributeNames";
    private static final String METHOD_CONVERTER_FOR_ATTRIBUTE = "converterForAttribute";
    private static final String METHOD_ATTRIBUTE_VALUE_TYPE = "attributeValueType";
    private static final String METHOD_TYPE = "type";
    private static final String METHOD_RAW_CLASS = "rawClass";
    private static final String FIELD_DELEGATE = "delegate";

    private static final String SQL_TIMESTAMP_CLASS = "java.sql.Timestamp";
    private static final String SQL_DATE_CLASS = "java.sql.Date";
    private static final String SQL_TIME_CLASS = "java.sql.Time";

    private static final String WARN_INSPECT_SCHEMA_MESSAGE = "Failed to inspect DynamoDB table schema converters for table '%s': %s";

    private static final Map<String, DynamoDbAttributeFormat> CONVERTER_FORMATS = createConverterFormats();
    private static final Map<String, Map<String, DynamoDbAttributeFormat>> formats = new ConcurrentHashMap<>();

    /**
     * Utility class; no instances.
     */
    private DynamoDbSchemaRegistry() {}

    /**
     * Builds the static mapping from built-in AWS SDK Enhanced Client converter class names
     * to their corresponding {@link DynamoDbAttributeFormat} values.
     *
     * @return unmodifiable map of converter FQCNs to attribute formats
     */
    private static Map<String, DynamoDbAttributeFormat> createConverterFormats() {
        Map<String, DynamoDbAttributeFormat> map = new HashMap<>();
        map.put(INSTANT_AS_STRING_CONVERTER, DynamoDbAttributeFormat.DATE_TIME);
        map.put(OFFSET_DATE_TIME_AS_STRING_CONVERTER, DynamoDbAttributeFormat.DATE_TIME);
        map.put(ZONED_DATE_TIME_AS_STRING_CONVERTER, DynamoDbAttributeFormat.DATE_TIME);
        map.put(LOCAL_DATE_TIME_CONVERTER, DynamoDbAttributeFormat.LOCAL_DATE_TIME);
        map.put(LOCAL_DATE_CONVERTER, DynamoDbAttributeFormat.DATE);
        map.put(LOCAL_TIME_CONVERTER, DynamoDbAttributeFormat.LOCAL_TIME);
        map.put(UUID_CONVERTER, DynamoDbAttributeFormat.UUID);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Inspects an enhanced client {@code TableSchema} via reflection and records inferred
     * attribute formats for the given table. Replaces any previously registered formats for
     * {@code table}; custom or unsupported converters keep scalar fallback.
     *
     * @param table DynamoDB table name, ignored when {@code null}
     * @param schema AWS SDK Enhanced {@code TableSchema} instance, or {@code null} to clear metadata
     */
    public static void register(String table, Object schema) {
        if (table == null) {
            SimpleLogger.uniqueWarn("Ignoring DynamoDB schema registration with null table name");
            return;
        }
        Map<String, DynamoDbAttributeFormat> found = new LinkedHashMap<>();
        if (schema != null) {
            try {
                Object names = DynamoDbReflectionHelper.invokeNoArg(schema, METHOD_ATTRIBUTE_NAMES);
                if (names instanceof Collection<?>) {
                    Method converterMethod = schema.getClass().getMethod(METHOD_CONVERTER_FOR_ATTRIBUTE, Object.class);
                    if (!Modifier.isPublic(converterMethod.getDeclaringClass().getModifiers())) {
                        converterMethod.setAccessible(true);
                    }
                    for (Object name : (Collection<?>) names) {
                        if (name == null) {
                            continue;
                        }
                        Object converter = converterMethod.invoke(schema, name);
                        if (converter != null) {
                            DynamoDbAttributeFormat format = resolveConverterFormat(converter);
                            if (format != null) {
                                found.put(String.valueOf(name), format);
                            }
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
                SimpleLogger.uniqueWarn(String.format(WARN_INSPECT_SCHEMA_MESSAGE, table, cause));
            }
        }
        formats.put(table, Collections.unmodifiableMap(found));
    }

    /**
     * Resolves the {@link DynamoDbAttributeFormat} for a single enhanced attribute converter.
     * Unwraps {@code OptionalAttributeConverter} wrappers, checks known SDK converter class names,
     * and otherwise inspects {@code attributeValueType()} and raw Java class metadata.
     *
     * @param rawConverter attribute converter instance returned by {@code converterForAttribute}
     * @return inferred attribute format, or {@code null} to keep ordinary scalar handling
     * @throws ReflectiveOperationException if reflective inspection fails
     */
    private static DynamoDbAttributeFormat resolveConverterFormat(Object rawConverter) throws ReflectiveOperationException {
        Object effective = unwrapOptionalConverter(rawConverter);
        DynamoDbAttributeFormat format = CONVERTER_FORMATS.get(effective.getClass().getName());
        if (format != null) {
            return format;
        }
        Object attributeValueType = DynamoDbReflectionHelper.invokeNoArg(effective, METHOD_ATTRIBUTE_VALUE_TYPE);
        if (attributeValueType == null) {
            return null;
        }
        String avType = attributeValueType instanceof Enum<?>
                ? ((Enum<?>) attributeValueType).name()
                : String.valueOf(attributeValueType);
        // AWS SDK v2 AtomicIntegerAttributeConverter mistakenly reports AttributeValueType.S even though it reads/writes N.
        if (ATOMIC_INTEGER_CONVERTER.equals(effective.getClass().getName())) {
            avType = DynamoDbAttributeType.NUMBER.getToken();
        }
        if (DynamoDbAttributeType.STRING.getToken().equals(avType)) {
            return resolveStringFormat(resolveConverterRawClass(effective));
        }
        if (DynamoDbAttributeType.NUMBER.getToken().equals(avType)) {
            return resolveIntegerFormat(resolveConverterRawClass(effective));
        }
        return null;
    }

    /**
     * Unwraps nested {@code OptionalAttributeConverter} instances by reading their {@code delegate}
     * field, guarding against cycles with identity tracking.
     *
     * @param converter outermost converter instance
     * @return innermost wrapped converter, or {@code converter} itself if not wrapped
     */
    private static Object unwrapOptionalConverter(Object converter) {
        Object current = converter;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current != null && visited.add(current)) {
            if (!OPTIONAL_CONVERTER.equals(current.getClass().getName())) {
                break;
            }
            Object delegate = readDeclaredField(current, FIELD_DELEGATE);
            if (delegate == null) {
                break;
            }
            current = delegate;
        }
        return current;
    }

    /**
     * Reads a declared field from {@code target} or one of its superclasses via reflection.
     *
     * @param target object whose field is read
     * @param fieldName declared field name
     * @return field value, or {@code null} if the field cannot be found or accessed
     */
    private static Object readDeclaredField(Object target, String fieldName) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts the target Java {@link Class} handled by an enhanced converter by invoking
     * {@code converter.type().rawClass()}.
     *
     * @param converter attribute converter instance
     * @return raw {@link Class} object, or {@code null} if unavailable
     */
    private static Object resolveConverterRawClass(Object converter) {
        Object enhancedType = DynamoDbReflectionHelper.invokeNoArg(converter, METHOD_TYPE);
        return DynamoDbReflectionHelper.invokeNoArg(enhancedType, METHOD_RAW_CLASS);
    }

    /**
     * Maps a string-backed DynamoDB attribute's Java raw class to a temporal or UUID format.
     *
     * @param rawClass candidate {@link Class} object from converter metadata
     * @return matching {@link DynamoDbAttributeFormat}, or {@code null} for plain/unrecognized types
     */
    private static DynamoDbAttributeFormat resolveStringFormat(Object rawClass) {
        if (!(rawClass instanceof Class<?>)) {
            return null;
        }
        Class<?> cls = (Class<?>) rawClass;
        String name = cls.getName();
        if (cls == Instant.class || cls == OffsetDateTime.class || cls == ZonedDateTime.class
                || cls == Date.class || SQL_TIMESTAMP_CLASS.equals(name)) {
            return DynamoDbAttributeFormat.DATE_TIME;
        }
        if (cls == LocalDateTime.class) {
            return DynamoDbAttributeFormat.LOCAL_DATE_TIME;
        }
        if (cls == LocalDate.class || SQL_DATE_CLASS.equals(name)) {
            return DynamoDbAttributeFormat.DATE;
        }
        if (cls == LocalTime.class || SQL_TIME_CLASS.equals(name)) {
            return DynamoDbAttributeFormat.LOCAL_TIME;
        }
        if (cls == UUID.class) {
            return DynamoDbAttributeFormat.UUID;
        }
        return null;
    }

    /**
     * Maps a number-backed DynamoDB attribute's Java raw class to a bounded or arbitrary-precision
     * integer format; floating-point and decimal types return {@code null}.
     *
     * @param rawClass candidate {@link Class} object from converter metadata
     * @return matching integer {@link DynamoDbAttributeFormat}, or {@code null} if non-integer
     */
    private static DynamoDbAttributeFormat resolveIntegerFormat(Object rawClass) {
        if (!(rawClass instanceof Class<?>)) {
            return null;
        }
        Class<?> cls = (Class<?>) rawClass;
        if (cls == byte.class || cls == Byte.class) {
            return DynamoDbAttributeFormat.INT8;
        }
        if (cls == short.class || cls == Short.class) {
            return DynamoDbAttributeFormat.INT16;
        }
        if (cls == int.class || cls == Integer.class || cls == AtomicInteger.class) {
            return DynamoDbAttributeFormat.INT32;
        }
        if (cls == long.class || cls == Long.class || cls == AtomicLong.class) {
            return DynamoDbAttributeFormat.INT64;
        }
        if (cls == BigInteger.class) {
            return DynamoDbAttributeFormat.INTEGER;
        }
        return null;
    }

    /**
     * Returns the recorded attribute formats for the specified DynamoDB table.
     *
     * @param table DynamoDB table name
     * @return unmodifiable map of attribute names to formats, or an empty map if none were recorded
     */
    public static Map<String, DynamoDbAttributeFormat> getFormats(String table) {
        return table == null ? Collections.emptyMap() : formats.getOrDefault(table, Collections.emptyMap());
    }
}
