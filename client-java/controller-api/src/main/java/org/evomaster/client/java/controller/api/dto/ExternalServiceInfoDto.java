package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

public class ExternalServiceInfoDto {

    /**
     * Hostname of the remote service captured in the external call
     */
    public String remoteHostname;

    /**
     * Refers to the protocol used in the external service calls.
     * Usually HTTP/S
     */
    public String protocol;

    /**
     * Port of the remote service
     */
    public Integer remotePort;

    public ExternalServiceInfoDto(){};

    public ExternalServiceInfoDto(String protocol, String remoteHostname, Integer remotePort) {
        this.protocol = protocol;
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalServiceInfoDto that = (ExternalServiceInfoDto) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(protocol, that.protocol) && Objects.equals(remotePort, that.remotePort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, protocol, remotePort);
    }
}
