package org.evomaster.core.problem.rest.arazzo.models

sealed class FailureReusable {
    data class Inline(val action: FailureAction) : FailureReusable()

    data class ReusableObj(val reusable: Reusable) : FailureReusable()
}