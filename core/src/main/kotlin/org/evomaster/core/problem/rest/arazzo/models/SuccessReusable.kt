package org.evomaster.core.problem.rest.arazzo.models

sealed class SuccessReusable {
    data class Inline(val action: SuccessAction) : SuccessReusable()

    data class ReusableObj(val reusable: Reusable) : SuccessReusable()
}