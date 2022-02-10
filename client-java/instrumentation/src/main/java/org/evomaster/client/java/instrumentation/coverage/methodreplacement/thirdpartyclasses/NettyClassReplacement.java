package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class NettyClassReplacement extends ThirdPartyMethodReplacementClass {

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.netty.handler.codec.http.HttpVersion";
    }

    @Replacement(replacingStatic = false,
        type = ReplacementType.TRACKER,
        id = "isKeepAliveDefault_boolean_class",
        usageFilter = UsageFilter.ONLY_SUT)
    public static boolean isKeepAliveDefault(Object caller) { return true; }
}
