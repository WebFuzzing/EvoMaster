package org.evomaster.client.java.instrumentation.shared;

public class ExternalServiceSharedUtils {

    public static final String DEFAULT_WM_SIGNATURE_HTTPS  = "__DEFAULT__WM_SIGNATURE__HTTPS__";

    public static final String DEFAULT_WM_SIGNATURE_HTTP  = "__DEFAULT__WM_SIGNATURE__HTTP__";

    public static final String DEFAULT_SOCKET_CONNECT_PROTOCOL = "TCP";

    public static String getSignature(String protocol, String remoteHostname, int remotePort){
        return protocol +"__" + remoteHostname + "__" + remotePort;
    }

    public static boolean isHttps(String protocol, int remotePort){
        if (protocol.equals(DEFAULT_SOCKET_CONNECT_PROTOCOL))
            return remotePort == 443 || remotePort == 8443;

        return protocol.equalsIgnoreCase("https");
    }

    public static String getWMDefaultSignature(String protocol, int remotePort){
        if (isHttps(protocol, remotePort))
            return DEFAULT_WM_SIGNATURE_HTTPS;
        return DEFAULT_WM_SIGNATURE_HTTP;
    }

    public static int getDefaultWMPort(String defaultSignature){
        if (defaultSignature.equals(DEFAULT_WM_SIGNATURE_HTTPS))
            return getDefaultWMHttpsPort();
        else if(defaultSignature.equals(DEFAULT_WM_SIGNATURE_HTTP))
            return getDefaultWMHttpPort();
        throw new IllegalArgumentException("it is not default signature");
    }

    public static int getDefaultWMHttpPort(){
        return 4200;
    }

    public static int getDefaultWMHttpsPort(){
        return 8443;
    }

    public static boolean isDefaultSignature(String signature){
        return signature.equals(DEFAULT_WM_SIGNATURE_HTTPS) || signature.equals(DEFAULT_WM_SIGNATURE_HTTP);
    }


}
