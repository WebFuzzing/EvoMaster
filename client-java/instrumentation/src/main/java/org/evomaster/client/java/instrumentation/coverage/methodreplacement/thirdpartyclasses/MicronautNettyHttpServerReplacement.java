package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import javax.servlet.ServletInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MicronautNettyHttpServerReplacement extends ThirdPartyMethodReplacementClass  {

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.micronaut.http.server.netty.NettyHttpServer";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "isKeepAlive_boolean_class",
            usageFilter = UsageFilter.ONLY_SUT)
    public static boolean isKeepAlive(Object caller) { return true; }

}
