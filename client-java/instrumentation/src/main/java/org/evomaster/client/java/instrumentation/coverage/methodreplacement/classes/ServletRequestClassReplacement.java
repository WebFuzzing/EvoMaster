package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServletRequestClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ServletRequestClassReplacement singleton = new ServletRequestClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "javax.servlet.ServletRequest";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "getInputStream")
    public static ServletInputStream getInputStream(Object caller) throws IOException {

        //TODO

        Method original = getOriginal(singleton, "getInputStream");

        try {
            return (ServletInputStream) original.invoke(caller);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }
}
