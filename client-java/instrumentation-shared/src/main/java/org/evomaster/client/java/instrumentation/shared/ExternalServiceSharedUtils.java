package org.evomaster.client.java.instrumentation.shared;

public class ExternalServiceSharedUtils {

    public static String getSignature(String protocol, String remoteHostname, int remotePort){
        return protocol +"__" + remoteHostname + "__" + remotePort;
    }
}
