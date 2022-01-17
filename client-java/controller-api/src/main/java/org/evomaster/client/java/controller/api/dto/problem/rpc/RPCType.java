package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * a list of RPC type supported by evomaster
 *
 * with different types, the handling might be different,
 * eg, create instance for input, exception handling and extraction
 */
public enum RPCType {
    /**
     * general handling on RPC interface,
     * e.g., return is response, and parameters are inputs
     */
    GENERAL,

    /**
     * thrift
     */
    THRIFT,

    /**
     * gRPC
     * TODO, NOT SUPPORT YET
     */
    gRPC
}
