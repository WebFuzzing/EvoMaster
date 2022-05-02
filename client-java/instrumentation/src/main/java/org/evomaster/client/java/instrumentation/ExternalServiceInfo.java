package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Information related to the external service calls
 */
public class ExternalServiceInfo implements Serializable {

    /**
     * Contains the remote hostname
     */
    private final String remoteHostname;

    private final String protocol;

    private final Integer remotePort;

    /*
        Hostname information about mock server
     */
    private final String mockHostname;

    /*
        Port of the mock server
     */
    private final Integer mockHostPort;

    public ExternalServiceInfo(String protocol, String remoteHostname, int remotePort, String mockHostname, int mockHostPort) {
        this.protocol = protocol;
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
        this.mockHostname = mockHostname;
        this.mockHostPort = mockHostPort;
    }

    public String getHostname() {
        return remoteHostname;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public String getMockHostname() {
        return mockHostname;
    }

    public Integer getMockHostPort() {
        return mockHostPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalServiceInfo that = (ExternalServiceInfo) o;
        return remotePort == that.remotePort && mockHostPort == that.mockHostPort && Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(protocol, that.protocol) && Objects.equals(mockHostname, that.mockHostname);
    }

    public ExternalServiceInfo copy(){
        return new ExternalServiceInfo(protocol, remoteHostname, remotePort, mockHostname, mockHostPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, protocol, remotePort, mockHostname, mockHostPort);
    }
}
