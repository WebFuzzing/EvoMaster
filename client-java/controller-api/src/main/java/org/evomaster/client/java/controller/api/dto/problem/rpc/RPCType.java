package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * A list of RPC types supported by EvoMaster
 *
 * With different types, the handling might be different,
 * eg, create instance for input, exception handling and extraction.
 *
 * If your type is not listed here, use the default [GENERAL]
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
     */
    gRPC
}
