package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class JacksonObjectMapperClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JacksonObjectMapperClassReplacement singleton = new JacksonObjectMapperClassReplacement();

    /*
        Jackson JavaType subclasses analyzed through the type of their elements. Not ArrayType: like a
        Foo[] class, it gets a schema of type array.
     */
    private static final String COLLECTION_LIKE_TYPE = "CollectionLikeType";
    private static final String COLLECTION_TYPE = "CollectionType";

    // JavaType methods, called by reflection
    private static final String GET_CONTENT_TYPE = "getContentType";
    private static final String GET_RAW_CLASS = "getRawClass";

    private static final String READ_BYTES_CLASS_ID = "Jackson_ObjectMapper_readValue_byte_array_Generic_class";
    private static final String READ_BYTES_JAVA_TYPE_ID = "Jackson_ObjectMapper_readValue_byte_array_JavaType_class";
    private static final String READ_BYTES_TYPE_REFERENCE_ID = "Jackson_ObjectMapper_readValue_byte_array_TypeReference_class";
    private static final String READ_STREAM_TYPE_REFERENCE_ID = "Jackson_ObjectMapper_readValue_InputStream_TypeReference_class";

    /*
        To read the mapper's AUTO_CLOSE_SOURCE setting by reflection. The parameter type is matched by the end
        of its name only, so the shade plugin has no package name to relocate.
     */
    private static final String GET_FACTORY = "getFactory";
    private static final String IS_ENABLED = "isEnabled";
    private static final String PARSER_FEATURE_CLASS_SUFFIX = "JsonParser$Feature";
    private static final String AUTO_CLOSE_SOURCE = "AUTO_CLOSE_SOURCE";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        /*
            this is to avoid a 2013 bug in shade plugin:
            https://issues.apache.org/jira/browse/MSHADE-156
            note that Jackson is shaded in pom of controller
         */
        return "   com.fasterxml.jackson.databind.ObjectMapper".trim();
    }


    /**
     * Register the schema of the type a JSON document is read into, and check whether the document
     * is a tainted value. Also used by {@link JacksonObjectReaderClassReplacement}.
     *
     * @param content the document, or null if it is known not to be a tainted value, in which case only the
     *                schema is registered
     * @param isArray if true, valueType is the type of the elements, and not of the collection itself
     */
    static void analyzeClass(Class<?> valueType, String content, boolean isArray){
        ClassToSchema.registerSchemaIfNeeded(valueType);
        if (content != null) {
            JsonTaint.handlePossibleJsonTaint(content, valueType, isArray);
        }
    }



    /*
        TODO:
        ideally, should provide method replacements for every single "readValue(...)" implementation.
        These can call each other (after modifying and preparing the inputs), and, as Jackson itself is instrumented,
        it should not be a problem.
        But we have seen issues in internal changes of Jackson, eg 2.11.0 vs 2.9.6, in which this would not work.
     */



    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_InputStream_Generic_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, InputStream src, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        String content = JsonUtils.readStream(src);
        analyzeClass(valueType, content,false);
        src = JsonUtils.toStream(content);

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_Generic_class", caller);

        try {
            return (T) original.invoke(caller, src, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_InputStream_JavaType_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, InputStream src,
                                  @ThirdPartyCast(actualType = "  com.fasterxml.jackson.databind.JavaType") Object valueType)
            throws Throwable {
        Objects.requireNonNull(caller);

        String content = JsonUtils.readStream(src);
        analyzeJavaType(content, valueType);
        src = JsonUtils.toStream(content);

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_JavaType_class", caller);

        try {
            return (T) original.invoke(caller, src, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_String_JavaType_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue_EM_0(Object caller, String content,
                                  @ThirdPartyCast(actualType = "  com.fasterxml.jackson.databind.JavaType") Object valueType)
            throws Throwable {
        Objects.requireNonNull(caller);

        analyzeJavaType(content, valueType);

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_String_JavaType_class", caller);

        try {
            return (T) original.invoke(caller, content, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Same as {@link #analyzeClass(Class, String, boolean)}, but for a Jackson JavaType, given as Object
     * as Jackson is not on the classpath of the instrumentation. For collections, the type of the elements
     * is analyzed. Also used by {@link JacksonObjectReaderClassReplacement}.
     */
    static void analyzeJavaType(String content, Object valueType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String typeName = valueType.getClass().getSimpleName();
        if(typeName.equals(COLLECTION_LIKE_TYPE) || typeName.equals(COLLECTION_TYPE)){
            Object contentType =  valueType.getClass().getMethod(GET_CONTENT_TYPE).invoke(valueType);
            Class<?> typeClass =  (Class) contentType.getClass().getMethod(GET_RAW_CLASS).invoke(contentType);
            analyzeClass(typeClass, content, true);
        } else {
            Class<?> typeClass = (Class) valueType.getClass().getMethod(GET_RAW_CLASS).invoke(valueType);
            analyzeClass(typeClass, content, false);
        }
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_String_TypeReference_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue_EM_1(Object caller, String content,
                                  @ThirdPartyCast(actualType = "  com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef)
            throws Throwable {

        Objects.requireNonNull(caller);

        Object javaType = constructJavaType(caller, valueTypeRef);
        if(javaType == null){
            Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_String_TypeReference_class", caller);

            try {
                return (T) original.invoke(caller, content, valueTypeRef);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        return readValue_EM_0(caller, content, javaType);
    }

    /**
     * Resolve a TypeReference into a JavaType the way Jackson does, ie, {@code _typeFactory.constructType(valueTypeRef)}.
     *
     * @return the JavaType, or null if the mapper has no _typeFactory field to read it from
     */
    private static Object constructJavaType(Object caller, Object valueTypeRef) throws IllegalAccessException, InvocationTargetException {
        Class<?> classObjectMapper = caller.getClass();
        while(!classObjectMapper.getSimpleName().equals("ObjectMapper")) {
            classObjectMapper = classObjectMapper.getSuperclass();
            if(classObjectMapper == null){
                //shouldn't really be possible
                SimpleLogger.error("EvoMaster instrumentation wrongly applied to " + caller.getClass().getName());
                break;
            }
        }
        Field _typeFactoryField = null;
        if(classObjectMapper != null){
            try {
                _typeFactoryField = classObjectMapper.getDeclaredField("_typeFactory");
            }catch (NoSuchFieldException e){
                SimpleLogger.warn("" + classObjectMapper.getName() + " does not have a field called _typeFactory");
            }
        }
        if(_typeFactoryField == null){
            return null;
        }

        _typeFactoryField.setAccessible(true);
        Object _typeFactory = _typeFactoryField.get(caller);
        Method constructType = Arrays.stream(_typeFactory.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals("constructType"))
                .filter(m -> m.getParameterTypes().length == 1)
                .filter(m-> m.getParameterTypes()[0].getName().endsWith("TypeReference"))
                .findFirst().orElse(null);
        return constructType.invoke(_typeFactory, valueTypeRef);
    }



    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_String_Generic_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, String content, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        analyzeClass(valueType, content, false);

        // JSON can be unwrapped using different approaches
        // val dto: FooDto = mapper.readValue(json)
        // To support this way, Jackson should be used inside the instrumentation
        // as shaded dependency, and that creates new problems.
        // Note: For now it's not supported

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_String_Generic_class", caller);

        try {
            return (T) original.invoke(caller, content, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_convertValue_Generic_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T convertValue(Object caller, Object fromValue, Class<T> toValueType) throws Throwable {
        Objects.requireNonNull(caller);

        ClassToSchema.registerSchemaIfNeeded(toValueType);

        if (fromValue instanceof String) {
            boolean isArray = false; //TODO
            JsonTaint.handlePossibleJsonTaint((String) fromValue, toValueType, isArray);
        }

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_convertValue_Generic_class", caller);

        try {
            return (T) original.invoke(caller, fromValue, toValueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


    /*
        The byte[] variants are what message converters use (eg spring-kafka's JsonMessageConverter), as a
        message payload arrives as bytes. None of them delegates to another public readValue in Jackson 2.x,
        so a call is analyzed once.
        The schema of the target type is always registered; the bytes are decoded only for the taint check,
        see JsonUtils.decodeUtf8IfCouldBeTaint. Jackson always gets the original input, so its own encoding
        detection is unaffected. A type that cannot be resolved by reflection is logged, and the analysis
        skipped.
     */

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_BYTES_CLASS_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, byte[] src, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        if (src != null && valueType != null) {
            analyzeClass(valueType, JsonUtils.decodeUtf8IfCouldBeTaint(src), false);
        }

        return (T) invokeOriginal(READ_BYTES_CLASS_ID, caller, src, valueType);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_BYTES_JAVA_TYPE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue_EM_2(Object caller, byte[] src,
                                       @ThirdPartyCast(actualType = "  com.fasterxml.jackson.databind.JavaType") Object valueType)
            throws Throwable {
        Objects.requireNonNull(caller);

        if (src != null && valueType != null) {
            try {
                analyzeJavaType(JsonUtils.decodeUtf8IfCouldBeTaint(src), valueType);
            } catch (ReflectiveOperationException | RuntimeException e) {
                logAnalysisFailure(caller, e);
            }
        }

        return (T) invokeOriginal(READ_BYTES_JAVA_TYPE_ID, caller, src, valueType);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_BYTES_TYPE_REFERENCE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue_EM_3(Object caller, byte[] src,
                                       @ThirdPartyCast(actualType = "  com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef)
            throws Throwable {
        Objects.requireNonNull(caller);

        if (src != null && valueTypeRef != null) {
            try {
                // resolved with the same type factory Jackson then uses for the read
                Object javaType = constructJavaType(caller, valueTypeRef);
                if (javaType != null) {
                    analyzeJavaType(JsonUtils.decodeUtf8IfCouldBeTaint(src), javaType);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                logAnalysisFailure(caller, e);
            }
        }

        return (T) invokeOriginal(READ_BYTES_TYPE_REFERENCE_ID, caller, src, valueTypeRef);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = READ_STREAM_TYPE_REFERENCE_ID,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue_EM_4(Object caller, InputStream src,
                                       @ThirdPartyCast(actualType = "  com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef)
            throws Throwable {
        Objects.requireNonNull(caller);

        Object javaType = null;
        if (src != null && valueTypeRef != null) {
            try {
                javaType = constructJavaType(caller, valueTypeRef);
            } catch (ReflectiveOperationException | RuntimeException e) {
                logAnalysisFailure(caller, e);
            }
        }
        if (javaType == null) {
            return (T) invokeOriginal(READ_STREAM_TYPE_REFERENCE_ID, caller, src, valueTypeRef);
        }

        /*
            The stream can be read only once, so Jackson gets a new one over the same bytes. Closing it
            closes the original, so AUTO_CLOSE_SOURCE still applies to the caller's stream.
         */
        byte[] content;
        try {
            content = JsonUtils.readAllBytes(src);
        } catch (IOException | RuntimeException e) {
            // Jackson would close the source here too, as its parser does on a failed read
            if (isAutoCloseSource(caller)) {
                try {
                    src.close();
                } catch (IOException closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            throw e;
        }
        try {
            analyzeJavaType(JsonUtils.decodeUtf8IfCouldBeTaint(content), javaType);
        } catch (ReflectiveOperationException | RuntimeException e) {
            logAnalysisFailure(caller, e);
        }

        return (T) invokeOriginal(READ_STREAM_TYPE_REFERENCE_ID, caller, new SourceClosingStream(content, src), valueTypeRef);
    }

    /**
     * Log, once per distinct message, that the target type of a read could not be resolved.
     * Also used by {@link JacksonObjectReaderClassReplacement}.
     */
    static void logAnalysisFailure(Object caller, Exception e) {
        // a reflective call wraps the real failure, and has no message of its own
        Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
        SimpleLogger.uniqueWarn("Failed to resolve the target type of a read with " + caller.getClass().getName() + ": " + cause);
    }

    /**
     * @return whether the mapper's factory has AUTO_CLOSE_SOURCE enabled. If that cannot be read, true,
     * which is Jackson's default.
     */
    private static boolean isAutoCloseSource(Object mapper) {
        try {
            Object factory = mapper.getClass().getMethod(GET_FACTORY).invoke(mapper);
            for (Method m : factory.getClass().getMethods()) {
                if (m.getName().equals(IS_ENABLED)
                        && m.getParameterTypes().length == 1
                        && m.getParameterTypes()[0].isEnum()
                        && m.getParameterTypes()[0].getName().endsWith(PARSER_FEATURE_CLASS_SUFFIX)) {
                    Object feature = enumConstant(m.getParameterTypes()[0], AUTO_CLOSE_SOURCE);
                    return (Boolean) m.invoke(factory, feature);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            logAnalysisFailure(mapper, e);
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> enumType, String name) {
        return Enum.valueOf((Class) enumType, name);
    }

    /**
     * A stream over bytes already read from another stream, which closes that other stream when closed.
     */
    private static class SourceClosingStream extends ByteArrayInputStream {

        /**
         * The stream the bytes were read from
         */
        private final InputStream source;

        SourceClosingStream(byte[] content, InputStream source) {
            super(content);
            this.source = Objects.requireNonNull(source);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                source.close();
            }
        }
    }

    /**
     * Call the replaced Jackson method, rethrowing whatever it throws as it is.
     */
    private static Object invokeOriginal(String id, Object caller, Object... inputs) throws Throwable {
        Method original = getOriginal(singleton, id, caller);

        try {
            return original.invoke(caller, inputs);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
