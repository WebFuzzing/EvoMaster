package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

public class ExternalServiceInfoDto {

    public String remoteHostname;

    public String protocol;

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
