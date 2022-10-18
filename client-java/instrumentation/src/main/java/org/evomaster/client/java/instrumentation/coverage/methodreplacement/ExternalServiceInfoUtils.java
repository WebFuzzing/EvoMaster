package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public class ExternalServiceInfoUtils {

    /**
     * @param remoteHostInfo is the host info collected from the SUT
     * @param remotePort     is the port employed by the SUT
     * @return redirected an array with two elements
     */
    public static String[] collectExternalServiceInfo(ExternalServiceInfo remoteHostInfo, int remotePort) {
        if (remoteHostInfo.getHostname().equals("info.services.auth.zalando.com"))
            return new String[]{remoteHostInfo.getHostname(), "" + remotePort};

        ExecutionTracer.addExternalServiceHost(remoteHostInfo);

        String signature = remoteHostInfo.signature();
        int connectPort = remotePort;
        if (!ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
            ExecutionTracer.addEmployedDefaultWMHost(remoteHostInfo);
            signature = ExternalServiceSharedUtils.getWMDefaultSignature(remoteHostInfo.getProtocol(), remotePort);
            connectPort = ExternalServiceSharedUtils.getDefaultWMPort(signature);
        }

        return new String[]{ExecutionTracer.getExternalMapping(signature), "" + connectPort};
    }

    /**
     * skip method replacement for some hostname, eg,
     */
    public static boolean skipHostnameOrIp(String hostname){
        return hostname.isEmpty() || hostname.startsWith("localhost") || hostname.startsWith("0.0.0") || hostname.startsWith("127.0.0.1");
    }


    /**
     *  Unless the port number is specified in a URL, the default will be -1.
     *  This indicates that the port should be assigned according to the
     *  protocol. Since the URLConnection openConnection is an abstract, this
     *  assignment will be handled under the respective implementation.
     *  Here it's manually handled assuming these default will never change.
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
