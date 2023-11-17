package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

public class HostnameResolutionInfo implements Serializable {
    private final String remoteHostname;

    private final String resolvedAddress;


    public HostnameResolutionInfo(String remoteHostname, String resolvedAddress) {
        this.remoteHostname = remoteHostname;
        this.resolvedAddress = resolvedAddress;
    }

    public String getHostname() {
        return remoteHostname;
    }

    public String getResolvedAddress() { return resolvedAddress; }

    /**
     * Will be true if the hostname resolved, otherwise false;
     */
    public Boolean getResolved() {
        return !resolvedAddress.equals(null);
    }

    public HostnameResolutionInfo copy(){
        return new HostnameResolutionInfo(remoteHostname, resolvedAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostnameResolutionInfo that = (HostnameResolutionInfo) o;
        return Objects.equals(remoteHostname, that.remoteHostname) && Objects.equals(resolvedAddress, that.resolvedAddress);
    }

    @Override
    public int hashCode() {
        // TODO: Excluded resolved from equals and hashCode, have to verify
        return Objects.hash(remoteHostname, resolvedAddress);
    }
}
