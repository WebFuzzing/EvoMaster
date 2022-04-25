package org.evomaster.client.java.controller.api.dto;

public class ExternalServiceInfoDto {

    public String remoteHostname;

    public String protocol;

    public Integer remotePort;

    public String mockHostname;

    public Integer mockHostPort;

    public ExternalServiceInfoDto(){};

    public ExternalServiceInfoDto(String protocol, String remoteHostname, Integer remotePort, String mockHostname, Integer mockHostPort) {
        this.protocol = protocol;
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
        this.mockHostname = mockHostname;
        this.mockHostPort = mockHostPort;
    }
}
