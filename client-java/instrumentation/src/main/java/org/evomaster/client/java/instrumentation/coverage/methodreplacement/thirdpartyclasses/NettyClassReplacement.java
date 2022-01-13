package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;


public class NettyClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final NettyClassReplacement singleton = new NettyClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.netty.handler.codec.http.HttpVersion";
    }

    @Replacement(replacingStatic = false,
        type = ReplacementType.TRACKER,
        id = "isKeepAliveDefault_boolean_class",
        usageFilter = UsageFilter.ANY)
    public boolean isKeepAliveDefault() {
        /*
            Micronaut 1.3.4 closes the connection when there is a server error
            (http status code > 299) or isKeepAliveDefault() set to false (default
            false). This method replacement is to change it to true.

            TODO: the implementation needs to be reviewed
        */
        return true;
    }
}
