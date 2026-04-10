package org.evomaster.core.problem.rest.arazzo.models

sealed class ParameterReusable {
    data class Param(val parameter: Parameter) : ParameterReusable()

    data class ReusableObj(val reusable: Reusable) : ParameterReusable()
}