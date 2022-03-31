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
        /**
         * If the protocol is HTTP/S and the remote is not localhost, then the
         * request will be remapped to WireMock instance running in a different
         * loopback.
         * */
        URLConnection urlConnection = null;
        if ((caller.getProtocol().equals("http") || caller.getProtocol().equals("https")) && !caller.getHost().equals("localhost")) {
                String url = "http://127.0.0.2:8080" + caller.getPath() + ((caller.getQuery() != null) ? "?" + caller.getQuery() : "");

                URL newUrl = new URL(url);
                urlConnection = newUrl.openConnection();
        } else {
            urlConnection = (URLConnection) caller.openConnection();
        }

        return urlConnection;
    }
}
