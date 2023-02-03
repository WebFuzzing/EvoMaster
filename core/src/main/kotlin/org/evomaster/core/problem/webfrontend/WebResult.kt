package org.evomaster.core.problem.webfrontend

import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

class WebResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)

    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return WebResult(this)
    }

    override fun matchedType(action: Action): Boolean {
        return action is WebAction
    }
}