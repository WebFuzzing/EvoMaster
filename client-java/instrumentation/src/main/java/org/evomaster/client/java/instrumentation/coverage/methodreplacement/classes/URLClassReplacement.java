package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.net.URL;
import java.net.URLConnection;

public class URLClassReplacement implements MethodReplacementClass {

    /**
     * This is an experimental implementation, not perfect yet.
     * */

    @Override
    public Class<?> getTargetClass() {
        return URL.class;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            id="URL_openConnection_Replacement",
            replacingStatic = false,
            usageFilter = UsageFilter.ANY
    )
    public static URLConnection openConnection(URL caller) throws java.io.IOException {
        if (caller == null) {
            throw new NullPointerException();
        }

        return caller.openConnection();
    }
}
