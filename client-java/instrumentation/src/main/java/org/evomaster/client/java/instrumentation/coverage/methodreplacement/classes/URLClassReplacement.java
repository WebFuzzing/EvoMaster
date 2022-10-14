package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.net.*;
import java.util.Objects;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils.collectExternalServiceInfo;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.NumberParsingUtils.parseLongHeuristic;

public class URLClassReplacement implements MethodReplacementClass {


    private static ThreadLocal<URL> instance = new ThreadLocal<>();


    /**
     * This is an experimental implementation, not perfect yet.
     * */

    @Override
    public Class<?> getTargetClass() {
        return URL.class;
    }


    public static URL consumeInstance(){

        URL url = instance.get();
        if(url == null){
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return url;
    }

    private static void addInstance(URL x){
        URL url = instance.get();
        if(url != null){
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(
            type = ReplacementType.EXCEPTION,
            category = ReplacementCategory.EXT_0,
            replacingConstructor = true
    )
    public static void URL(String s, String idTemplate) throws MalformedURLException {
        URL(null, s, null, idTemplate);
    }

    @Replacement(
            type = ReplacementType.EXCEPTION,
            category = ReplacementCategory.EXT_0,
            replacingConstructor = true
    )
    public static void URL(URL context, String s, String idTemplate) throws MalformedURLException {
        URL(context, s, null, idTemplate);
    }

    @Replacement(
            type = ReplacementType.EXCEPTION,
            category = ReplacementCategory.EXT_0,
            replacingConstructor = true
    )
    public static void URL(URL context, String s, URLStreamHandler handler, String idTemplate) throws MalformedURLException {

        if (ExecutionTracer.isTaintInput(s)) {
            ExecutionTracer.addStringSpecialization(s,
                    new StringSpecializationInfo(StringSpecialization.URL, null));
        }

        URL url;

        if (idTemplate == null) {
            url =  new java.net.URL(context,s,handler);
        } else {

            try {
                URL res = new java.net.URL(context,s,handler);
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                        new Truthness(1, DistanceHelper.H_NOT_NULL));
                url = res;
            } catch (RuntimeException e) {
                double h = s == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
                throw e;
            }
        }

        addInstance(url);
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
            int port = caller.getPort();
            String protocol = caller.getProtocol();

            // Unless the port number is specified, the default will be -1.
            // Which indicates that the port should be assigned according to the
            // protocol. Since the URLConnection openConnection is an abstract, this
            // assignment will be handled under the respective implementation.
            // Here it's manually handled assuming these default will never change. :)
            if (port == -1) {
                switch (protocol) {
                    case "https":
                        port = 443;
                        break;
                    case "http":
                        port = 80;
                        break;
                }
            }
            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(protocol, caller.getHost(), port);
            String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, port);

            // Usage of ports below 1024 require root privileges to run
            String url = caller.getProtocol()+"://" + ipAndPort[0]+":"+ipAndPort[1] + caller.getPath();

            URL newURL = new URL(url);
            return newURL.openConnection();
        }
        if (!caller.getProtocol().equals("jar") && !caller.getProtocol().equals("file"))
            SimpleLogger.uniqueWarn("not handle the protocol with:"+caller.getProtocol());
        return caller.openConnection();
    }
}
