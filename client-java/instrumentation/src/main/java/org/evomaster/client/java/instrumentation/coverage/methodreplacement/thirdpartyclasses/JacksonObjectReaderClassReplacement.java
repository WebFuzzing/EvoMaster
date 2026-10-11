package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.JsonUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Replacements for reading JSON with a Jackson ObjectReader, eg one built with
 * {@code objectMapper.readerFor(type)}, as {@link JacksonObjectMapperClassReplacement} does for
 * ObjectMapper. This is how spring-kafka's JsonDeserializer reads every record value, and library
 * code is instrumented too.
 * <p>
 * Only the target type configured in the reader is known: a reader without one is not analyzed.
 */
public class JacksonObjectReaderClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JacksonObjectReaderClassReplacement singleton = new JacksonObjectReaderClassReplacement();

    /**
     * Public getter of the reader's target type, added in Jackson 2.10
     */
    private static final String GET_VALUE_TYPE = "getValueType";

    /**
     * Field holding the reader's target type in every Jackson 2.x version, read when the getter is missing
     */
    private static final String VALUE_TYPE_FIELD = "_valueType";

    private static final String READ_BYTES_ID = "Jackson_ObjectReader_readValue_byte_array";
    private static final String READ_STRING_ID = "Jackson_ObjectReader_readValue_String";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        /*
            this is to avoid a 2013 bug in shade plugin:
            https://issues.apache.org/jira/browse/MSHADE-156
            note that Jackson is shaded in pom of controller
         */
        return "   com.fasterxml.jackson.databind.ObjectReader".trim();
    }

    /*
        Neither calls another replaced readValue, so a call is analyzed once. readValue(byte[]|String, Class)
        calls forType(valueType).readValue(content), so it is analyzed here too.
     */

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_BYTES_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, byte[] content) throws Throwable {
        Objects.requireNonNull(caller);

        if (content != null) {
            analyzeTargetType(caller, JsonUtils.decodeUtf8IfCouldBeTaint(content));
        }

        return (T) invokeOriginal(singleton, READ_BYTES_ID, caller, content);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_STRING_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, String content) throws Throwable {
        Objects.requireNonNull(caller);

        if (content != null) {
            analyzeTargetType(caller, content);
        }

        return (T) invokeOriginal(singleton, READ_STRING_ID, caller, content);
    }

    /**
     * Analyze the content against the target type configured in the reader, if any. A null content only
     * registers the schema. A type that cannot be read is logged, and the analysis skipped.
     */
    private static void analyzeTargetType(Object reader, String content) {
        try {
            Object valueType = getValueType(reader);
            if (valueType == null) {
                // a reader built without a target type: nothing to analyze
                return;
            }
            JacksonObjectMapperClassReplacement.analyzeJavaType(content, valueType);
        } catch (ReflectiveOperationException | RuntimeException e) {
            JacksonObjectMapperClassReplacement.logAnalysisFailure(reader, e);
        }
    }

    /**
     * @return the JavaType the reader reads into, or null if it was built without one
     */
    private static Object getValueType(Object reader) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            Method getter = reader.getClass().getMethod(GET_VALUE_TYPE);
            return getter.invoke(reader);
        } catch (NoSuchMethodException e) {
            // Jackson before 2.10. The field can be declared in a superclass, if the reader is a subclass.
            Class<?> klass = reader.getClass();
            while (klass != null) {
                try {
                    Field field = klass.getDeclaredField(VALUE_TYPE_FIELD);
                    field.setAccessible(true);
                    return field.get(reader);
                } catch (NoSuchFieldException notHere) {
                    klass = klass.getSuperclass();
                }
            }
            throw e;
        }
    }
}
