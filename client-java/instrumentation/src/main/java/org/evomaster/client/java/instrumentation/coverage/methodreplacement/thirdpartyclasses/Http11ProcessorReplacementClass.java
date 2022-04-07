package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

public class Http11ProcessorReplacementClass extends ThirdPartyMethodReplacementClass {

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.apache.coyote.http11.Http11Processor";
    }

    @Replacement(replacingStatic = true,
            type = ReplacementType.TRACKER,
            id = "statusDropsConnection",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.BASE)
    public static boolean statusDropsConnection(int code){
        /*
         *  Never drop a TCP connection to EvoMaster during the search,
         *  regardless of HTTP status codes
         */
        return false;
    }


}
