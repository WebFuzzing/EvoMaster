package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

public class MicronautNettyHttpServerReplacement extends ThirdPartyMethodReplacementClass  {

    private static final MicronautNettyHttpServerReplacement singleton = new MicronautNettyHttpServerReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.micronaut.http.server.netty.NettyHttpServer";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "isKeepAlive_boolean_class",
            usageFilter = UsageFilter.ONLY_SUT)
    public boolean isKeepAlive() {
        /*
            Micronaut 1.3.4 closes the connection when there is a server error
            (in micronaut case it checks for http status code > 299) or isKeepAliveDefault()
            set to false (default false). This method replacement is to change it to true.

            TODO: the implementation needs to be reviewed
        */
        return true;
    }

}
