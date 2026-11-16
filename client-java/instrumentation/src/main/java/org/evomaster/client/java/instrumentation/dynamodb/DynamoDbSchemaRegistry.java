package org.evomaster.client.java.instrumentation.dynamodb;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent instrumentation metadata, refreshed whenever an enhanced table is mapped. */
public final class DynamoDbSchemaRegistry {
    private static final String CONVERTER_PACKAGE =
            "software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.";

    /**
     * Maps each fully-qualified SDK {@code AttributeConverter} class name to its {@link DynamoDbAttributeFormat}
     * for the most common constrained string converters in {@code DefaultAttributeConverterProvider}.
     * <p>
     * Less commonly used constrained string converters intentionally left for later:
     * {@code CharacterAttributeConverter}, {@code UriAttributeConverter}, {@code UrlAttributeConverter},
     * {@code PeriodAttributeConverter}, {@code MonthDayAttributeConverter}, {@code ZoneIdAttributeConverter},
     * {@code ZoneOffsetAttributeConverter}, and {@code LocaleAttributeConverter} (as well as wrapper or
     * parameterized converters such as {@code OptionalAttributeConverter} and {@code EnumAttributeConverter}).
     */
    private static final Map<String, DynamoDbAttributeFormat> CONVERTER_FORMATS = createConverterFormats();

    /** Maps each DynamoDB table name to an immutable map from attribute name to {@link DynamoDbAttributeFormat}. */
    private static final Map<String, Map<String, DynamoDbAttributeFormat>> formats = new ConcurrentHashMap<>();

    private DynamoDbSchemaRegistry() {}

    /**
     * Builds the static converter lookup table.
     *
     * @return immutable map from converter class name to attribute format
     */
    private static Map<String, DynamoDbAttributeFormat> createConverterFormats() {
        Map<String, DynamoDbAttributeFormat> map = new HashMap<>();
        map.put(CONVERTER_PACKAGE + "InstantAsStringAttributeConverter", DynamoDbAttributeFormat.DATE_TIME);
        map.put(CONVERTER_PACKAGE + "OffsetDateTimeAsStringAttributeConverter", DynamoDbAttributeFormat.DATE_TIME);
        map.put(CONVERTER_PACKAGE + "ZonedDateTimeAsStringAttributeConverter", DynamoDbAttributeFormat.DATE_TIME);
        map.put(CONVERTER_PACKAGE + "LocalDateTimeAttributeConverter", DynamoDbAttributeFormat.LOCAL_DATE_TIME);
        map.put(CONVERTER_PACKAGE + "LocalDateAttributeConverter", DynamoDbAttributeFormat.DATE);
        map.put(CONVERTER_PACKAGE + "LocalTimeAttributeConverter", DynamoDbAttributeFormat.LOCAL_TIME);
        map.put(CONVERTER_PACKAGE + "UuidAttributeConverter", DynamoDbAttributeFormat.UUID);
        return Collections.unmodifiableMap(map);
    }

    /** Records only known converter semantics; custom converters retain the scalar fallback. */
    public static void register(String table, Object schema) {
        // Maps each attribute name on the registered table to its resolved format.
        Map<String, DynamoDbAttributeFormat> found = new LinkedHashMap<>();
        try {
            Object names = DynamoDbReflectionHelper.invokeNoArg(schema, "attributeNames");
            if (names instanceof Collection<?>) {
                for (Object name : (Collection<?>) names) {
                    Object converter = schema.getClass().getMethod("converterForAttribute", Object.class).invoke(schema, name);
                    if (converter != null) {
                        DynamoDbAttributeFormat format = CONVERTER_FORMATS.get(converter.getClass().getName());
                        if (format != null) {
                            found.put(String.valueOf(name), format);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Older SDKs and document schemas may not expose converters.
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            SimpleLogger.uniqueWarn("Failed to inspect DynamoDB table schema converters for table '" + table + "': " + cause);
        }
        formats.put(table, Collections.unmodifiableMap(found));
    }

    /**
     * Returns immutable metadata suitable for serialization with an intercepted query.
     *
     * @param table DynamoDB table name
     * @return immutable map from attribute name to format, or an empty map when unavailable
     */
    public static Map<String, DynamoDbAttributeFormat> getFormats(String table) {
        return table == null ? Collections.emptyMap() : formats.getOrDefault(table, Collections.emptyMap());
    }
}
