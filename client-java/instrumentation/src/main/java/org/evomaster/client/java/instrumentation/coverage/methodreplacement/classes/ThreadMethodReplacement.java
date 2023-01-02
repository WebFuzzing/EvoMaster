package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public class ThreadMethodReplacement implements MethodReplacementClass {

    @Override
    public Class<?> getTargetClass() {
        return Thread.class;
    }

    @Replacement(replacingStatic = true,
            type = ReplacementType.TRACKER,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0,
            packagesToSkip = {"org.apache.catalina.","org.apache.coyote"} //internal in Tomcat server
    )
    public static void sleep(long millis) throws InterruptedException {
        sleep(millis,0);
    }

    @Replacement(replacingStatic = true,
            type = ReplacementType.TRACKER,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0,
            packagesToSkip = {"org.apache.catalina.","org.apache.coyote"} //internal in Tomcat server
    )
    public static void sleep(long millis, int nanos) throws InterruptedException {

        int limit = 1000;

        if(millis >= limit){
            millis = limit;
            nanos = 0;
        }

        ExecutionTracer.reportSleeping();

        Thread.sleep(millis, nanos);
    }
}
