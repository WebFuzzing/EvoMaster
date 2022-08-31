package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, InputStream src, Class<T> valueType) {
        Objects.requireNonNull(caller);

        if (valueType != null) {
            String name = valueType.getName();
            String schema = ClassToSchema.getOrDeriveSchema(valueType);
            UnitsInfoRecorder.registerNewParsedDto(name, schema);
            ExecutionTracer.addParsedDtoName(name);
        }

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_InputStream_class", caller);

        try {
            return (T) original.invoke(caller, src, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "Jackson_ObjectMapper_readValue_Generic_class",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.EXT_0)
    public static <T> T readValue(Object caller, String content, Class<T> valueType) {
        Objects.requireNonNull(caller);

        if (valueType != null) {
            String name = valueType.getName();
            String schema = ClassToSchema.getOrDeriveSchema(valueType);
            UnitsInfoRecorder.registerNewParsedDto(name, schema);
            ExecutionTracer.addParsedDtoName(name);
        }

        // JSON can be unwrapped using different approaches
        // val dto: FooDto = mapper.readValue(json)
        // To support this way, Jackson should be used inside the instrumentation
        // as shaded dependency. And that crates new problems.
        // Note: For now it's not supported

        Method original = getOriginal(singleton, "Jackson_ObjectMapper_readValue_Generic_class", caller);

        try {
            return (T) original.invoke(caller, content, valueType);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }
}
