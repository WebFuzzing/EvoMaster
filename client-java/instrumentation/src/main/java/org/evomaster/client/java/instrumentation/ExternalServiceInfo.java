package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * Information related to the external service calls
 */
public class ExternalServiceInfo implements Serializable {

    /**
     * Contains the remote hostname
     */
    private final String remoteHostname;

    private final int remotePort;

    private final String mockHostname;

    private final int mockHostPort;

    public ExternalServiceInfo(String remoteHostname, int remotePort, String mockHostname, int mockHostPort) {
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
        this.mockHostname = mockHostname;
        this.mockHostPort = mockHostPort;
    }

    public String getHostname() {
        return remoteHostname;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getMockHostname() {
        return mockHostname;
    }

    public int getMockHostPort() {
        return mockHostPort;
    }

}
