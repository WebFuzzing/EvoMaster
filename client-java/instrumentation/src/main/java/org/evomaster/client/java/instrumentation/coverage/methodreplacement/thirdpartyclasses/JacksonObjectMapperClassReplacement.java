package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;


import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class JacksonObjectMapperClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JacksonObjectMapperClassReplacement singleton = new JacksonObjectMapperClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        /*
            this is to avoid a 2013 bug in shade plugin:
            https://issues.apache.org/jira/browse/MSHADE-156
            note that Jackson is shaded in pom of controller
         */
        return "   com.fasterxml.jackson.databind.ObjectMapper".trim();
    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_File_Generic_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, File src, Class<T> valueType) throws Throwable {
//        // Read the JSON from the given file path
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueType);
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_File_Generic_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }
//
//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_File_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, File src, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        // Read the JSON from the given file path
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueTypeRef.getClass());
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_File_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_URL_Generic_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, URL src, Class<T> valueType) throws Throwable {
//        // Used to fetch JSON from a URL
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueType);
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_URL_Generic_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_URL_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, URL src, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        // Used to fetch JSON from a URL
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueTypeRef.getClass());
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_URL_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Reader_Generic_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, Reader src, Class<T> valueType) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//
//        if (src instanceof StringReader) {
//            JsonTaint.handlePossibleJsonTaint(src.toString(), valueType);
//        }
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Reader_Generic_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Reader_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, Reader src, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//
//        if (src instanceof StringReader) {
//            JsonTaint.handlePossibleJsonTaint(src.toString(), valueTypeRef.getClass());
//        }
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Reader_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_InputStream_Generic_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, InputStream src, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        ClassToSchema.registerSchemaIfNeeded(valueType);

        String content = new BufferedReader(
                new InputStreamReader(src, Charset.defaultCharset()))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));

        JsonTaint.handlePossibleJsonTaint(content, valueType);

        src = new ByteArrayInputStream(content.getBytes());

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_Generic_class", caller);

        try {
            return (T) original.invoke(caller, src, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_InputStream_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, InputStream src,  @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//
//        String content = new BufferedReader(
//                new InputStreamReader(src, Charset.defaultCharset()))
//                .lines()
//                .collect(Collectors.joining(System.lineSeparator()));
//
//        JsonTaint.handlePossibleJsonTaint(content, valueTypeRef.getClass());
//
//        src = new ByteArrayInputStream(content.getBytes());
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Byte_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, byte[] src, Class<T> valueType) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        String content = new String(src);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//        JsonTaint.handlePossibleJsonTaint(content, valueType);
//
//        src = content.getBytes();
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Byte_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Byte_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, byte[] src, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        String content = new String(src);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//        JsonTaint.handlePossibleJsonTaint(content, valueTypeRef.getClass());
//
//        src = content.getBytes();
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Byte_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Byte_Length_Generic_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, byte[] src, int offset, int len, Class<T> valueType) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//
//        String content = new String(src);
//
//        JsonTaint.handlePossibleJsonTaint(content, valueType);
//
//        src = content.getBytes();
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Byte_Length_Generic_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, offset, len, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_Byte_Length_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, byte[] src, int offset, int len, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//
//        String content = new String(src);
//
//        JsonTaint.handlePossibleJsonTaint(content, valueTypeRef.getClass());
//
//        src = content.getBytes();
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Byte_Length_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, offset, len, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_DataInput_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, DataInput src, Class<T> valueType) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType);
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueType);
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_DataInput_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_DataInput_JavaType_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, DataInput src, @ThirdPartyCast(actualType = "com.fasterxml.jackson.databind.JavaType") Object valueType) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueType.getClass());
//        JsonTaint.handlePossibleJsonTaint(src.toString(), valueType.getClass());
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_DataInput_JavaType_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

//    @Replacement(replacingStatic = false,
//            type = ReplacementType.TRACKER,
//            id = "Jackson_ObjectMapper_readValue_String_TypeReference_class",
//            usageFilter = UsageFilter.ANY,
//            category = ReplacementCategory.EXT_0)
//    public static <T> T readValue(Object caller, String content, @ThirdPartyCast(actualType = "com.fasterxml.jackson.core.type.TypeReference") Object valueTypeRef) throws Throwable {
//        Objects.requireNonNull(caller);
//
//        ClassToSchema.registerSchemaIfNeeded(valueTypeRef.getClass());
//        JsonTaint.handlePossibleJsonTaint(content, valueTypeRef.getClass());
//
//        // JSON can be unwrapped using different approaches
//        // val dto: FooDto = mapper.readValue(json)
//        // To support this way, Jackson should be used inside the instrumentation
//        // as shaded dependency. And that crates new problems.
//        // Note: For now it's not supported
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_String_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, content, valueTypeRef);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw e.getCause();
//        }
//    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_String_Generic_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, String content, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        ClassToSchema.registerSchemaIfNeeded(valueType);
        JsonTaint.handlePossibleJsonTaint(content, valueType);

        // JSON can be unwrapped using different approaches
        // val dto: FooDto = mapper.readValue(json)
        // To support this way, Jackson should be used inside the instrumentation
        // as shaded dependency. And that crates new problems.
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
    public static <T> T convertValue(Object caller, Object fromValue, Class<T> valueType) throws Throwable {
        Objects.requireNonNull(caller);

        ClassToSchema.registerSchemaIfNeeded(valueType);

        if (fromValue instanceof String) {
            JsonTaint.handlePossibleJsonTaint((String) fromValue, valueType);
        }

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_convertValue_Generic_class", caller);

        try {
            return (T) original.invoke(caller, fromValue, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
