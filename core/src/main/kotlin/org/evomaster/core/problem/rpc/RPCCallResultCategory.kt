package org.evomaster.core.problem.rpc

enum class RPCCallResultCategory {
    /**
     * potential faults by an RPC call
     * eg, internal error by Thrift
     */
    POTENTIAL_FAULT,

    /**
     * a successful RPC call
     */
    SUCCESS,

    /**
     * a customized exception thrown by an RPC call
     */
    CUSTOM_EXCEPTION,

    /**
     * an exception thrown by an RPC call
     */
    EXCEPTION,

    /**
     * failed to process an RPC call
     */
    FAILED

    // business logic reward here

    // might add category representing an exception due to inputs
}