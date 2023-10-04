package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

public class HostnameInfo implements Serializable {
    private final String remoteHostname;

    /**
     * Will be true if the hostname resolved, otherwise false;
     */
    private final Boolean resolved;

    public HostnameInfo(String remoteHostname, Boolean resolved) {
        this.remoteHostname = remoteHostname;
        this.resolved = resolved;
    }

    public String getHostname() {
        return remoteHostname;
    }

    public Boolean getResolved() {
        return resolved;
    }
}
