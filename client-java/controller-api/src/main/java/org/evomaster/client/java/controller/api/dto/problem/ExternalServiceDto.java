package org.evomaster.client.java.controller.api.dto.problem;

public class ExternalServiceDto {

    /**
     * The hostname of the external service.
     * Note: it should not be a raw IP address
     */
    public  String hostname;

    /**
     * TCP/UDP port. If negative, the port is ignored
     */
    public int port;
}
