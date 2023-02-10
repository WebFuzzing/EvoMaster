package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public class ExternalServiceInfoUtils {

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

        if (!ExecutionTracer.hasMockServer(remoteHostInfo.getHostname())) {
            String signature = remoteHostInfo.signature();
            int connectPort = remotePort;

            if (!ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
                ExecutionTracer.addEmployedDefaultWMHost(remoteHostInfo);
                signature = ExternalServiceSharedUtils.getWMDefaultSignature(remoteHostInfo.getProtocol(), remotePort);
                connectPort = ExternalServiceSharedUtils.getDefaultWMPort(signature);
            }

            return new String[]{ExecutionTracer.getExternalMapping(signature), "" + connectPort};
        } else {
            return new String[]{remoteHostInfo.getHostname(), "" + remotePort};
        }
    }

    /**
     * skip method replacement for some hostname, eg,
     */
    public static boolean skipHostnameOrIp(String hostname) {
        // https://en.wikipedia.org/wiki/Reserved_IP_addresses
        // There could be other possible ranges to ignore since it is not
        // necessary for the moment, following IP address ranges are skipped
        if (hostname.isEmpty()
                || hostname.startsWith("localhost")
                || hostname.startsWith("0.0.0")
                || hostname.startsWith("10.")
                || hostname.startsWith("docker.socket")
                || (hostname.startsWith("127.") && !ExecutionTracer.hasLocalAddressReplacement(hostname))) {
            return true;
        }

        return false;
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
}
