package org.evomaster.client.java.controller.api.dto.problem.rpc.exception;

/**
 * a list of RPC exception categories
 */
public enum RPCExceptionCategory {
    /**
     * exception due to application
     */
    APPLICATION,

    /**
     * exception due to transport
     * eg, time out, port is not open
     */
    TRANSPORT,

    /**
     * exception due to protocol
     * eg, incorrect input which does not conform the schema
     */
    PROTOCOL,

    /**
     * unclear or unclassified
     */
    OTHERS
}
