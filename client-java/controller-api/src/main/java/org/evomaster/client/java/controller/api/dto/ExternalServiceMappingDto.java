package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

public class ExternalServiceMappingDto {

    public String remoteHostname;

    public String localIPAddress;

    public String signature;

    public Boolean isActive;

    public ExternalServiceMappingDto(){};

    public ExternalServiceMappingDto(String remoteHostname, String localIPAddress, String signature, Boolean isActive) {
        this.remoteHostname = remoteHostname;
        this.localIPAddress = localIPAddress;
        this.signature = signature;
        this.isActive = isActive;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalServiceMappingDto that = (ExternalServiceMappingDto) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(localIPAddress, that.localIPAddress) && Objects.equals(signature, that.signature) && Objects.equals(isActive, that.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteHostname, localIPAddress, signature, isActive);
    }
}
