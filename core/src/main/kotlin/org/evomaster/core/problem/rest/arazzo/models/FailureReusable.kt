package org.evomaster.core.problem.rest.arazzo.models

sealed class FailureReusable {
    data class Failure(val action: FailureAction) : FailureReusable()

    data class ReusableObj(val reusable: Reusable) : FailureReusable()
}