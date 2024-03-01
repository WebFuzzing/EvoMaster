package org.evomaster.client.java.instrumentation;

public class ExternalServiceMapping {

    private final String remoteHostName;

    private final String localIPAddress;

    /**
     * Signature of the local mock service replaced the external web
     * service.
     */
    private final String signature;

    /**
     * Indicate the state of the mock server, whether it's active or not.
     */
    private final Boolean isActive;


    public ExternalServiceMapping(String remoteHostName, String localIPAddress, String signature, Boolean isActive) {
        this.remoteHostName = remoteHostName;
        this.localIPAddress = localIPAddress;
        this.signature = signature;
        this.isActive = isActive;
    }

    public String getLocalIPAddress() {
        return localIPAddress;
    }

    public String getRemoteHostname() {
        return remoteHostName;
    }

    public String getSignature() {
        return signature;
    }

    public Boolean isActive() {
        return isActive;
    }
}
