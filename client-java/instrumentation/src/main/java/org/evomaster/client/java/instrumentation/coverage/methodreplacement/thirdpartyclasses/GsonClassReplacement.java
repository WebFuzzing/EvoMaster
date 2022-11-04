package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    public static Object fromJson(Object caller, String json, Class<?> classOfT){

        if(caller == null){
            throw new NullPointerException();
        }

        ClassToSchema.registerSchemaIfNeeded(classOfT);
        JsonTaint.handlePossibleJsonTaint(json,classOfT);

        Method original = getOriginal(singleton, "fromJson_string_class", caller);

        try {
            return original.invoke(caller, json, classOfT);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

}
