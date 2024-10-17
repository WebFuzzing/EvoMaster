package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;


import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.CharBuffer;

public class GsonClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final GsonClassReplacement singleton = new GsonClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.google.gson.Gson";
    }

    // TODO all versions of fromJson

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "fromJson_string_class",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE)
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
            category = ReplacementCategory.BASE
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
            category = ReplacementCategory.BASE
    )
    public static Object fromJson(Object caller, Reader json, Class<?> classOfT) {
        if (caller == null) {
            throw new NullPointerException();
        }

        ClassToSchema.registerSchemaIfNeeded(classOfT);
        JsonTaint.handlePossibleJsonTaint(getStringFromReader(json), classOfT);

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
            category = ReplacementCategory.BASE
    )
    public static Object fromJson(Object caller, Reader json, Type typeOfT) {
        if (caller == null) {
            throw new NullPointerException();
        }

        Class<?> klass = (Class<?>) typeOfT;
        ClassToSchema.registerSchemaIfNeeded(klass);
        JsonTaint.handlePossibleJsonTaint(getStringFromReader(json), klass);

        Method original = getOriginal(singleton, "fromJson_reader_type", caller);

        try {
            return original.invoke(caller, json, klass);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private static String getStringFromReader(Reader reader) {
        String s = "";
        int character;

        try {
            while ((character = reader.read()) != -1) {
                s += (char) character;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return s;
    }

}
