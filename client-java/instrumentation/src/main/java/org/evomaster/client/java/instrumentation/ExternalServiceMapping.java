package org.evomaster.client.java.instrumentation;

public class ExternalServiceMapping {

    private String remoteHostName;

    private String localIPAddress;

    private String signature;

    private Boolean isActive;


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
