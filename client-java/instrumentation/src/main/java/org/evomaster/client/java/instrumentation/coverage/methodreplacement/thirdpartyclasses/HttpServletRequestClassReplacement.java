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
import java.util.Enumeration;

public class HttpServletRequestClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final HttpServletRequestClassReplacement singleton = new HttpServletRequestClassReplacement();



    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "javax.servlet.http.HttpServletRequest";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getInputStream",
            usageFilter = UsageFilter.ONLY_SUT,
            category = ReplacementCategory.BASE,
            castTo = "javax.servlet.ServletInputStream"
    )
    public static Object getInputStream(Object caller) throws IOException {

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.markRawAccessOfHttpBodyPayload();

        Method original = getOriginal(singleton, "getInputStream", caller);

        try {
            return original.invoke(caller);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getParameter",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0)
    public static String getParameter(Object caller, String param){

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.addQueryParameter(param);

        Method original = getOriginal(singleton, "getParameter", caller);

        try {
            return (String) original.invoke(caller, param);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getParameterValues",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static String[] getParameterValues(Object caller, String param){

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.addQueryParameter(param);

        Method original = getOriginal(singleton, "getParameterValues", caller);

        try {
            return (String[]) original.invoke(caller, param);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getHeader",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static String getHeader(Object caller, String header){

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.addHeader(header);

        Method original = getOriginal(singleton, "getHeader", caller);

        try {
            return (String) original.invoke(caller, header);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getHeaders",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0
    )
    public static Enumeration<String> getHeaders(Object caller, String header){

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.addHeader(header);

        Method original = getOriginal(singleton, "getHeaders", caller);

        try {
            return (Enumeration<String>) original.invoke(caller, header);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }
}
