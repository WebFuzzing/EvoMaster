package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

public class HostnameResolutionInfo implements Serializable {
    private final String remoteHostname;

    /**
     * Will be true if the hostname resolved, otherwise false;
     */
    private final Boolean resolved;

    public HostnameResolutionInfo(String remoteHostname, Boolean resolved) {
        this.remoteHostname = remoteHostname;
        this.resolved = resolved;
    }

    public String getHostname() {
        return remoteHostname;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public HostnameResolutionInfo copy(){
        return new HostnameResolutionInfo(remoteHostname, resolved);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostnameResolutionInfo that = (HostnameResolutionInfo) o;
        return Objects.equals(remoteHostname, that.remoteHostname);
    }

    @Override
    public int hashCode() {
        // TODO: Excluded resolved from equals and hashCode, have to verify
        return Objects.hash(remoteHostname);
    }
}
