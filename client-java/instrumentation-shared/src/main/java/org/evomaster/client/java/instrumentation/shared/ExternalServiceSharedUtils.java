package org.evomaster.client.java.instrumentation.shared;

public class ExternalServiceSharedUtils {

    /**
     * default WM signature for https
     */
    public static final String DEFAULT_WM_SIGNATURE_HTTPS  = "__DEFAULT__WM_SIGNATURE__HTTPS__";

    /**
     * default WM signature for http
     */
    public static final String DEFAULT_WM_SIGNATURE_HTTP  = "__DEFAULT__WM_SIGNATURE__HTTP__";

    /**
     * default protocol if the connection is built with socket
     */
    public static final String DEFAULT_SOCKET_CONNECT_PROTOCOL = "TCP";

    /**
     *
     * @return a signature with the given protocol, remote host name and port
     */
    public static String getSignature(String protocol, String remoteHostname, int remotePort){
        return protocol +"__" + remoteHostname + "__" + remotePort;
    }

    /**
     *
     * @return whether the port is for https protocol
     */
    public static boolean isHttps(String protocol, int remotePort){
        if (protocol.equals(DEFAULT_SOCKET_CONNECT_PROTOCOL))
            return remotePort == 443 || remotePort == 8443;

        return protocol.equalsIgnoreCase("https");
    }

    /**
     *
     * @return the default wm signature with the given protocol and port
     */
    public static String getWMDefaultSignature(String protocol, int remotePort){
        if (isHttps(protocol, remotePort))
            return DEFAULT_WM_SIGNATURE_HTTPS;
        return DEFAULT_WM_SIGNATURE_HTTP;
    }

    /**
     * @return the default port based on the given defaultSignature
     */
    public static int getDefaultWMPort(String defaultSignature){
        if (defaultSignature.equals(DEFAULT_WM_SIGNATURE_HTTPS))
            return getDefaultWMHttpsPort();
        else if(defaultSignature.equals(DEFAULT_WM_SIGNATURE_HTTP))
            return getDefaultWMHttpPort();
        throw new IllegalArgumentException("it is not default signature");
    }

    /**
     * @return the default port for http
     */
    public static int getDefaultWMHttpPort(){
        return 4200;
    }

    /**
     * @return the default port for https
     */
    public static int getDefaultWMHttpsPort(){
        return 8443;
    }

    /**
     * @return whether the signature is default wm signature
     */
    public static boolean isDefaultSignature(String signature){
        return signature.equals(DEFAULT_WM_SIGNATURE_HTTPS) || signature.equals(DEFAULT_WM_SIGNATURE_HTTP);
    }


}
