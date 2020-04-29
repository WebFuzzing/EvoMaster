package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GsonClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final GsonClassReplacement singleton = new GsonClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.google.gson.Gson";
    }

    // TODO all versions of fromJson

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "fromJson_string_class")
    public static Object fromJson(Object caller, String json, Class<?> classOfT){

        if(classOfT != null) {
            String schema = ClassToSchema.getOrDeriveSchema(classOfT);

            //TODO
        }

        Method original = getOriginal(singleton, "fromJson_string_class");

        try {
            return original.invoke(caller, json, classOfT);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }

}
