package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServletRequestClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ServletRequestClassReplacement singleton = new ServletRequestClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "javax.servlet.ServletRequest";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getInputStream",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.BASE,
            castTo = "javax.servlet.ServletInputStream"
    )
    public static  Object getInputStream(Object caller) throws Exception {

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.markRawAccessOfHttpBodyPayload();

        Method original = getOriginal(singleton, "getInputStream", caller);

        try {
            return  original.invoke(caller);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (Exception) e.getCause();
        }
    }
}
