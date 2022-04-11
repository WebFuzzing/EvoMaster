package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

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
            category = ReplacementCategory.NET,
            id = "URL_openConnection_Replacement",
            replacingStatic = false,
            usageFilter = UsageFilter.ANY
    )
    public static URLConnection openConnection(URL caller) throws java.io.IOException {
        Objects.requireNonNull(caller);

        /*
          Add the external service hostname to the ExecutionTracer
          */
        if (caller.getProtocol().equals("http") || caller.getProtocol().equals("https")) {
            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(caller.getProtocol(), caller.getHost(), caller.getPort(), "", -1);
            ExecutionTracer.addExternalServiceHost(remoteHostInfo);
        }

        return caller.openConnection();
    }
}
