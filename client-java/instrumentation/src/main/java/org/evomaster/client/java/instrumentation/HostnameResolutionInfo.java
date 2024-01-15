package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils;

import java.io.Serializable;
import java.util.Objects;

/**
    Information about whether a hostname does resolve or not to an IP address.
 */
public class HostnameResolutionInfo implements Serializable {
    private final String remoteHostname;

    private final String resolvedAddress;


    public HostnameResolutionInfo(String remoteHostname, String resolvedAddress) {

        if(remoteHostname == null || remoteHostname.isEmpty()){
            throw new IllegalArgumentException("Empty remoteHostName");
        }

        if(resolvedAddress != null && !ExternalServiceInfoUtils.isValidIP(resolvedAddress)){
            // IP address could null (ie not resolved). however, if specified, must be valid
            throw new IllegalArgumentException("Invalid resolved IP address: " + resolvedAddress);
        }

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
        return resolvedAddress != null;
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
        return Objects.hash(remoteHostname, resolvedAddress);
    }
}
