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

    private static final DocumentClassReplacement singleton = new DocumentClassReplacement();

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
            castTo = ORG_BSON_DOCUMENT)
    public static Object parse(String json) {

        // Get the parse method which takes a String as argument
        final Method documentParseMethod = getOriginalStaticMethod(singleton,"Document_parse_String");

        // Get the org.bson.Document class
        final Class<?> documentClass = documentParseMethod.getDeclaringClass();

        // register the taint for JSON format in this method
        JsonTaint.handlePossibleJsonTaint(json, documentClass, false);

        // Invoke the parse method and get the result
        try {
            return documentParseMethod.invoke(null, json);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }


}
