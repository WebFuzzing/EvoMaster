package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

// import javax.servlet.ServletInputStream;
// import java.io.IOException;
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;

public class JettyClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final JettyClassReplacement singleton = new JettyClassReplacement();

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
