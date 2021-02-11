package org.evomaster.core.problem.graphql

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.ActionResult

class GraphQlCallResult : HttpWsCallResult {

    constructor(stopping: Boolean = false) : super(stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    companion object {
    }

    override fun copy(): ActionResult {
        return GraphQlCallResult(this)
    }

}
