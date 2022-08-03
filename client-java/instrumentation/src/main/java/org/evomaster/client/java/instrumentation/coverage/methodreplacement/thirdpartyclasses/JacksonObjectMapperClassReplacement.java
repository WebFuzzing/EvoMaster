package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JacksonObjectMapperClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JacksonObjectMapperClassReplacement singleton = new JacksonObjectMapperClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.fasterxml.jackson.databind.ObjectMapper";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "readValue_ObjectMapper_class",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.NET)
    public static <T> T readValue(Object caller, InputStream src, Class<T> valueType) throws IOException, JsonParseException, JsonMappingException {
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "readValue_ObjectMapper_class", caller);

        try {
            return (T) original.invoke(caller, src, valueType);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }
}
