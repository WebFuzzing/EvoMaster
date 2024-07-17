package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.object.JsonTaint;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DocumentClassReplacement extends ThirdPartyMethodReplacementClass {

    public static final String ORG_BSON_DOCUMENT = "org.bson.Document";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return ORG_BSON_DOCUMENT;
    }

    @Replacement(replacingStatic = true,
            type = ReplacementType.TRACKER,
            id = "Document_parse_String",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.MONGO,
            castTo = "org.bson.Document")
    public static Object parse(String json) {
        // Load the Document class
        Class<?> documentClass;
        try {
            documentClass = Class.forName("org.bson.Document");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonTaint.handlePossibleJsonTaint(json,documentClass);

        // Get the parse method which takes a String as argument
        Method parseMethod;
        try {
            parseMethod = documentClass.getDeclaredMethod("parse", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Invoke the parse method and get the result
        try {
            return parseMethod.invoke(null, json);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }
}
