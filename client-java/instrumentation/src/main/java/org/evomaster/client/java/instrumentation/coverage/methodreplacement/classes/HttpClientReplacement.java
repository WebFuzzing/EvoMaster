package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import sun.net.www.http.HttpClient;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;


public class HttpClientReplacement implements MethodReplacementClass {

    // Note: Ignore this

    @Override
    public Class<?> getTargetClass() {
        return HttpClient.class;
    }

    @Replacement(type = ReplacementType.TRACKER,
            usageFilter = UsageFilter.ONLY_SUT,
            replacingStatic = true)
    public static HttpClient New(URL url) throws IOException {
        HttpClient h = HttpClient.New(url);

        return h;
    }

    @Replacement(type = ReplacementType.TRACKER,
            usageFilter = UsageFilter.ONLY_SUT,
            replacingStatic = true)
    public static HttpClient New(URL url, boolean useCache) throws IOException {
        HttpClient h = HttpClient.New(url, useCache);

        return h;
    }

    @Replacement(type = ReplacementType.TRACKER,
            usageFilter = UsageFilter.ONLY_SUT,
            replacingStatic = true)
    public static HttpClient New(URL url, Proxy p, int to, boolean useCache,
                                 HttpURLConnection httpuc) throws IOException {
        HttpClient h = HttpClient.New(url, p, to, useCache, httpuc);

        return h;
    }

    @Replacement(type = ReplacementType.TRACKER,
//    usageFilter = UsageFilter.ONLY_SUT,
    replacingStatic = false)
    public static void openServer(HttpClient caller, String server, int port) throws IOException {
        // TODO
    }

}
