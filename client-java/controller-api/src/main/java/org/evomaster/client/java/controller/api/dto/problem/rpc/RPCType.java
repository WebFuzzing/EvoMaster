package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * created by manzhang on 2021/11/3
 */
public enum RPCType {
    /**
     * general handling on RPC interface,
     * e.g., return is response, and parameters are inputs
     */
    GENERAL,
    THRIFT,
    gRPC
}
