package org.evomaster.core.problem.rpc

enum class RPCCallResultCategory {
    /**
     * potential faults by an RPC call
     * eg, internal error by Thrift
     */
    INTERNAL_ERROR,

    /**
     * protocol error, eg, due to invalid inputs
     */
    PROTOCOL_ERROR,

    /**
     * transport error, eg, time out
     */
    TRANSPORT_ERROR,

    /**
     * unexpected exception
     * eg, runtime exception
     */
    UNEXPECTED_EXCEPTION,

    /**
     * a RPC call which is handed as defined and there is no any exception thrown
     */
    HANDLED,

    /**
     * a customized exception thrown by an RPC call
     */
    CUSTOM_EXCEPTION,

    /**
     * an exception thrown by an RPC call that is neither unexpected nor customized
     */
    OTHERWISE_EXCEPTION,

    /**
     * failed to process an RPC call
     */
    FAILED

}