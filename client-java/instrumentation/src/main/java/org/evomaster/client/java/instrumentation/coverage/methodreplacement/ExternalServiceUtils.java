package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.HostnameResolutionInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.InetAddressClassReplacement;
import org.evomaster.client.java.instrumentation.shared.IPAddressValidator;
import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.MethodReplacementPreserveSemantics;
import org.evomaster.client.java.utils.SimpleLogger;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

public class ExternalServiceUtils {

    /**
     * A local address created based on a direct IP address like 127.0.0.1 can result
     * in a reverse-lookup for hostname in something different than "localhost".
     * This could be for example "kubernetes.docker.internal" if you have Docker installed.
     */
    public static final String canonicalLocalHostname = InetAddress.getLoopbackAddress().getCanonicalHostName();

    /**
     * Check if string literal is a valid v4 or v6 IP address
     */
    public static boolean isValidIP(String s) {
        if (IPAddressValidator.isValidInet4Address(s)) {
            return true;
        }

        return IPAddressValidator.isValidInet6Address(s);
    }

    /**
     * Force collecting DNS info, without failing if errors
     */
    public static void analyzeDnsResolution(String host){

        if(skipHostnameOrIp(host)){
            return;
        }

        // TODO: If we skip Inet resolution at this point, in case if it is already exists, would
        //  it improve the speed?
        try {
            InetAddress addresses = InetAddressClassReplacement.getByName(host);
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, addresses.getHostAddress()));
        } catch (Exception e){
            //do nothing
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, null));
        }
    }


    /**
     * If there is a mock server assigned for the given hostname,
     * core will try to check the active status and if not active it will
     * update the respective port and initiate the server. On the next, attempt
     * SUT will connect to the active WireMock server.
     *
     * @param remoteHostInfo is the host info collected from the SUT
     * @param remotePort     is the port employed by the SUT
     * @return redirected an array with two elements
     */
    public static String[] collectExternalServiceInfo(ExternalServiceInfo remoteHostInfo, int remotePort) {
        // Note: Checking whether there is any active mapping or not will reduce the amount
        // of time the same info gets added again and again. To do this, have to change the
        // data structure of the external service mapping inside ExecutionTracer

        ExecutionTracer.addExternalServiceHost(remoteHostInfo);

        //FIXME need to check if no info on port

        if (!ExecutionTracer.hasLocalAddressForHost(remoteHostInfo.getHostname())) {
            return new String[]{ExecutionTracer.getDefaultSinkholeAddress(), "" + remotePort};
        } else {
            return new String[]{ExecutionTracer.getLocalAddress(remoteHostInfo.getHostname()), "" + remotePort};
        }
    }

    /**
     * skip method replacement for some hostname.
     * For example, we want to avoid skipping local addresses, because things like Databases and other
     * services like Kafka could be running there.
     * Further, those things could be running in Docker, so should skip Docker as well
     *
     */
    public static boolean skipHostnameOrIp(String hostname) {
        // https://en.wikipedia.org/wiki/Reserved_IP_addresses
        // There could be other possible ranges to ignore since it is not
        // necessary for the moment, following IP address ranges are skipped
        return hostname == null
                || hostname.isEmpty()
                || skipHostname(hostname)
                || hostname.startsWith("0.")
                || hostname.startsWith("10.")
                || hostname.startsWith("192.168.")
                // in some cases, we do not skip this, because
                || (hostname.startsWith("127.") && !ExecutionTracer.hasMappingForLocalAddress(hostname))
                ;
    }

    public static boolean skipHostname(String hostname){
        return hostname == null
                || hostname.isEmpty()
                || hostname.equals("localhost")
                || hostname.equals("docker.socket")
                || hostname.equals(canonicalLocalHostname)
                ;
    }


    /**
     * Unless the port number is specified in a URL, the default will be -1.
     * This indicates that the port should be assigned according to the
     * protocol. Since the URLConnection openConnection is an abstract, this
     * assignment will be handled under the respective implementation.
     * Here it's manually handled assuming these default will never change.
     *
     * @param port
     * @param protocol
     * @return
     */
    public static int inferPort(int port, String protocol) {

        if (port >= 0) {
            return port;
        }

        switch (protocol) {
            case "https":
                return 443;
            case "http":
                return 80;
        }

        return port;
    }


    public static URL getReplacedURL(URL url) {
        if (MethodReplacementPreserveSemantics.shouldPreserveSemantics) {
            return url;
        }

        URL replacedURL = url;
        if ((url.getProtocol().equalsIgnoreCase("http")
                || url.getProtocol().equalsIgnoreCase("https"))
                && !skipHostnameOrIp(url.getHost())
                && !ExecutionTracer.skipHostnameAndPort(url.getHost(), url.getPort())) {

            int port = ExternalServiceUtils.inferPort(url.getPort(), url.getProtocol());

            ExternalServiceUtils.analyzeDnsResolution(url.getHost());

            if (url.getProtocol().equalsIgnoreCase("https"))
                PreDefinedSSLInfo.setTrustAllForHttpsURLConnection();

            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(url.getProtocol(), url.getHost(), port);
            String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, port);

            // Usage of ports below 1024 require root privileges to run
            String urlString = url.getProtocol() + "://" + ipAndPort[0] + ":" + ipAndPort[1] + url.getPath();

            if (url.getQuery() != null)
                urlString += "?" + url.getQuery();

            try {
                replacedURL = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return replacedURL;
    }
}
