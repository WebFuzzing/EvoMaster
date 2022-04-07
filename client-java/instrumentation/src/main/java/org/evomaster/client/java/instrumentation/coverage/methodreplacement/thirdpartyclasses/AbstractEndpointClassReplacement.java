package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

public class AbstractEndpointClassReplacement extends ThirdPartyMethodReplacementClass {

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.apache.tomcat.util.net.AbstractEndpoint";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "getMaxKeepAliveRequests",
            usageFilter = UsageFilter.ANY,
             category = ReplacementCategory.BASE)
    public static int getMaxKeepAliveRequests(Object caller) {
        /*
            This is a problem, if Driver is not configuring this... as
            by default we would get TCP closed every 100 HTTP requests
         */
        return -1;
    }
}
