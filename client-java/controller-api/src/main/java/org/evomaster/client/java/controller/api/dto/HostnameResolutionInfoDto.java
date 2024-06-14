package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

public class HostnameResolutionInfoDto {

    public String remoteHostname;

    public String resolvedAddress;

    public HostnameResolutionInfoDto(){};

    public HostnameResolutionInfoDto(String remoteHostname, String resolvedAddress) {
        this.remoteHostname = remoteHostname;
        this.resolvedAddress = resolvedAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostnameResolutionInfoDto that = (HostnameResolutionInfoDto) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(resolvedAddress, that.resolvedAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, resolvedAddress);
    }
}
