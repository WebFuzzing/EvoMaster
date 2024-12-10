package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/*
    TODO likely to merge with ExternalServiceInfo
 */
public class ExternalService implements Serializable {

    /**
     * The hostname of the external service.
     * Note: it should not be a raw IP address
     */
    private final String hostname;

    /**
     * TCP/UDP port. If negative, the port is ignored
     */
    private final int port;

    public ExternalService(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        if(hostname == null || hostname.isEmpty()){
            throw new IllegalArgumentException("Invalid empty hostname");
        }
        if(port > 65536){
            throw new IllegalArgumentException("Too large port number: " + port);
        }
    }

    public ExternalService(String hostname) {
        this(hostname, -1);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
