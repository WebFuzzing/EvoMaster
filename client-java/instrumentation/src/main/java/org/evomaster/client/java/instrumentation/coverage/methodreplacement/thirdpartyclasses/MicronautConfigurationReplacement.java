package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MicronautConfigurationReplacement extends ThirdPartyMethodReplacementClass {

    private static final MicronautConfigurationReplacement singleton = new MicronautConfigurationReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.micronaut.http.server.netty.configuration.NettyHttpServerConfiguration";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "micronautNettyServerConfiguration_class",
            usageFilter = UsageFilter.ONLY_SUT)
    public boolean isKeepAliveOnServerError() {
        return true;
    }
}
