package org.evomaster.core.problem.rpc

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

/**
 * here, RPCCallResult is inherent from HttpWs for the moment
 * since some RPC might be based on HTTP, eg gRPC,
 * then we could reuse properties of HTTP results
 */
class RPCCallResult : HttpWsCallResult {

    constructor(stopping: Boolean = false) : super(stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return RPCCallResult(this)
    }

    override fun matchedType(action: Action): Boolean {
        return action is RPCCallAction
    }
}