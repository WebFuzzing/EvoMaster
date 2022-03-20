package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import sun.net.www.http.HttpClient;

import java.io.IOException;

public class HttpClientReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return HttpClient.class;
    }

    @Replacement(type = ReplacementType.TRACKER, usageFilter = UsageFilter.ONLY_SUT, replacingStatic = false)
    public void openServer(HttpClient caller, String server, int port) throws IOException {
        // TODO: Ideal place to replace server information when there is a HTTP request
        // Ignore this for now
    }
}
