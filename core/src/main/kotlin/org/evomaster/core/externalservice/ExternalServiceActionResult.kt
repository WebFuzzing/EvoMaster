package org.evomaster.core.externalservice

import org.evomaster.core.search.ActionResult

class ExternalServiceActionResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)
    constructor(other: ExternalServiceActionResult) : super(other)

    override fun copy() : ExternalServiceActionResult {
        return ExternalServiceActionResult(this)
    }

}