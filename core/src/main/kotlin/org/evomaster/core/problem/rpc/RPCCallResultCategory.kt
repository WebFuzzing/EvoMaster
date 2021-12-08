package org.evomaster.core.problem.rpc

enum class RPCCallResultCategory {
    P_BUG,
    SUCCESS,
    CUSTOM_EXCEPTION,
    EXCEPTION,
    FAILED

    // business logic reward here

    // might add category representing an exception due to inputs
}