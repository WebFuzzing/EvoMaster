package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WebRequestClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final WebRequestClassReplacement singleton = new WebRequestClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.springframework.web.context.request.WebRequest";
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getParameter",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE)
    public static String getParameter(Object caller, String param) throws Exception{

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
            throw (Exception) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getParameterValues",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE
    )
    public static String[] getParameterValues(Object caller, String param) throws Exception{

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
            throw (Exception) e.getCause();
        }
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getHeader",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE
    )
    public static String getHeader(Object caller, String header) throws Exception{

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
            throw (Exception) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getHeaderValues",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE
    )
    public static String[] getHeaderValues(Object caller, String header) throws Exception{

        if(caller == null){
            throw new NullPointerException();
        }

        ExecutionTracer.addHeader(header);

        Method original = getOriginal(singleton, "getHeaderValues", caller);

        try {
            return (String[]) original.invoke(caller, header);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (Exception) e.getCause();
        }
    }
}
