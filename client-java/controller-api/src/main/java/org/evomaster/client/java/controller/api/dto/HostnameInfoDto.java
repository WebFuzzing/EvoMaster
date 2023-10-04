package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

public class HostnameInfoDto {

    public String remoteHostname;

    public Boolean resolved;

    public HostnameInfoDto(){};

    public HostnameInfoDto(String remoteHostname, Boolean resolved) {
        this.remoteHostname = remoteHostname;
        this.resolved = resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostnameInfoDto that = (HostnameInfoDto) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(resolved, that.resolved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, resolved);
    }
}
