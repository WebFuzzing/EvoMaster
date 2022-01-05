package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

public class NettyClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final NettyClassReplacement singleton = new NettyClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.netty.handler.codec.http.HttpVersion";
    }

    @Replacement(replacingStatic = false,
        type = ReplacementType.TRACKER,
        id = "isKeepAliveDefault",
        usageFilter = UsageFilter.ONLY_SUT)
    public boolean isKeepAliveDefault() {
        return true;
    }
}
