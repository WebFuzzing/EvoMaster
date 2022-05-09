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

    public ExternalServiceInfo(String protocol, String remoteHostname, Integer remotePort) {
        this.protocol = protocol;
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalServiceInfo that = (ExternalServiceInfo) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(protocol, that.protocol) && Objects.equals(remotePort, that.remotePort);
    }

    public ExternalServiceInfo copy(){
        return new ExternalServiceInfo(protocol, remoteHostname, remotePort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, protocol, remotePort);
    }
}
