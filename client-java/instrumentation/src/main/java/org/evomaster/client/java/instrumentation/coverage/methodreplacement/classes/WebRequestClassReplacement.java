package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
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


    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "getParameter")
    public static String getParameter(Object caller, String param){

        ExecutionTracer.addQueryParameter(param);

        Method original = getOriginal(singleton, "getParameter");

        try {
            return (String) original.invoke(caller, param);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "getParameterValues")
    public static String[] getParameterValues(Object caller, String param){

        ExecutionTracer.addQueryParameter(param);

        Method original = getOriginal(singleton, "getParameterValues");

        try {
            return (String[]) original.invoke(caller, param);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }


    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "getHeader")
    public static String getHeader(Object caller, String header){

        ExecutionTracer.addHeader(header);

        Method original = getOriginal(singleton, "getHeader");

        try {
            return (String) original.invoke(caller, header);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "getHeaderValues")
    public static String[] getHeaderValues(Object caller, String header){

        ExecutionTracer.addHeader(header);

        Method original = getOriginal(singleton, "getHeaderValues");

        try {
            return (String[]) original.invoke(caller, header);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);// ah, the beauty of Java...
        }
    }
}
