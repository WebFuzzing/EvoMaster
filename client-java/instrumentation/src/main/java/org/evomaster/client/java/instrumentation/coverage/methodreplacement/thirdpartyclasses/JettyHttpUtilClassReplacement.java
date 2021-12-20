package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// import javax.servlet.ServletInputStream;
// import java.io.IOException;
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;

public class JettyHttpUtilClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JettyHttpUtilClassReplacement singleton = new JettyHttpUtilClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.netty.handler.codec.http.HttpUtil";
    }

    @Replacement(replacingStatic = true,
            type = ReplacementType.TRACKER,
            id = "setKeepAlive_void_class",
            usageFilter = UsageFilter.ONLY_SUT)
    public static Object setKeepAlive(Object caller, Object h, Object httpVersion, boolean keepAlive) {
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "setKeepAlive_void_class", caller);

        try {
            return original.invoke(caller, h, httpVersion, true);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }
}
