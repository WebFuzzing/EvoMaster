package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;

public class JacksonObjectMapperClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JacksonObjectMapperClassReplacement singleton = new JacksonObjectMapperClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.fasterxml.jackson.databind.ObjectMapper";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_InputStream_class",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.NET)
    public static <T> T readValue(Object caller, InputStream src, Class<T> valueType) {
        Objects.requireNonNull(caller);

        throw new RuntimeException("Method invoked InputStream");
//
//        if(valueType != null) {
//            String name = valueType.getName();
//            String schema = ClassToSchema.getOrDeriveSchema(valueType);
//            UnitsInfoRecorder.registerNewParsedDto(name, schema);
//            ExecutionTracer.addParsedDtoName(name);
//        }
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_class", caller);
//
//        try {
//            return (T) original.invoke(caller, src, valueType);
//        } catch (IllegalAccessException e){
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e){
//            throw (RuntimeException) e.getCause();
//        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_TypeReference_class",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.BASE)
    public static <T> T readValue(Object caller, String content, TypeReference<T> valueTypeRef) {
        Objects.requireNonNull(caller);
        throw new RuntimeException("Method invoked TyepReference");

//        if(valueTypeRef != null) {
//            // To make things work, same approach in Jackson is used to get the
//            //  information about the class.
//            Type genericType  = valueTypeRef.getType();
//            TypeFactory _typeFactory = TypeFactory.defaultInstance();
//            JavaType _javaType = _typeFactory.constructType(genericType);
//
//            String name = genericType.getTypeName();
//            String schema = ClassToSchema.getOrDeriveSchema(_javaType.getRawClass());
//            UnitsInfoRecorder.registerNewParsedDto(name, schema);
//            ExecutionTracer.addParsedDtoName(name);
//        }
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_TypeReference_class", caller);
//
//        try {
//            return (T) original.invoke(caller, content, valueTypeRef);
//        } catch (IllegalAccessException e){
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e){
//            throw (RuntimeException) e.getCause();
//        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_Generic_class",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.BASE)
    public static <T> T readValue(Object caller, String content, Class<T> valueType) {
        Objects.requireNonNull(caller);
        throw new RuntimeException("Method invoked Generic");
//
//        if(valueType != null) {
//            String name = valueType.getName();
//            String schema = ClassToSchema.getOrDeriveSchema(valueType);
//            UnitsInfoRecorder.registerNewParsedDto(name, schema);
//            ExecutionTracer.addParsedDtoName(name);
//        }
//
//        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Generic_class", caller);
//
//        try {
//            return (T) original.invoke(caller, content, valueType);
//        } catch (IllegalAccessException e){
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e){
//            throw (RuntimeException) e.getCause();
//        }
    }
}
