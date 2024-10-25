package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.JsonUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;


public class GsonClassReplacement extends ThirdPartyMethodReplacementClass {

    // TODO: Gson has three more methods, which use their custom classes
    //  JsonReader and JsonElement.
    //  Those are not supported yet.

    private static final GsonClassReplacement singleton = new GsonClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.google.gson.Gson";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "fromJson_string_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static Object fromJson(Object caller, String json, Class<?> classOfT) {

        if (caller == null) {
            throw new NullPointerException();
        }

        ClassToSchema.registerSchemaIfNeeded(classOfT);
        JsonTaint.handlePossibleJsonTaint(json, classOfT);

        Method original = getOriginal(singleton, "fromJson_string_class", caller);

        try {
            return original.invoke(caller, json, classOfT);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(
            replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "fromJson_string_type",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static Object fromJson(Object caller, String json, Type typeOfT) {
        if (caller == null) {
            throw new NullPointerException();
        }

        Class<?> klass = (Class<?>) typeOfT;
        ClassToSchema.registerSchemaIfNeeded(klass);
        JsonTaint.handlePossibleJsonTaint(json, klass);

        Method original = getOriginal(singleton, "fromJson_string_type", caller);

        try {
            return original.invoke(caller, json, typeOfT);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(
            replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "fromJson_reader_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static Object fromJson(Object caller, Reader json, Class<?> classOfT) {
        if (caller == null) {
            throw new NullPointerException();
        }

        ClassToSchema.registerSchemaIfNeeded(classOfT);
        String content = JsonUtils.getStringFromReader(json);
        JsonTaint.handlePossibleJsonTaint(content, classOfT);
        json = JsonUtils.stringToReader(content);

        Method original = getOriginal(singleton, "fromJson_reader_class", caller);

        try {
            return original.invoke(caller, json, classOfT);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(
            replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "fromJson_reader_type",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static Object fromJson(Object caller, Reader json, Type typeOfT) {
        if (caller == null) {
            throw new NullPointerException();
        }

        Class<?> klass = (Class<?>) typeOfT;
        ClassToSchema.registerSchemaIfNeeded(klass);
        String content = JsonUtils.getStringFromReader(json);
        JsonTaint.handlePossibleJsonTaint(content, klass);
        json = JsonUtils.stringToReader(content);

        Method original = getOriginal(singleton, "fromJson_reader_type", caller);

        try {
            return original.invoke(caller, json, typeOfT);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

}
