package org.evomaster.core.problem.rpc

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

/**
 * created by manzhang on 2021/11/26
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