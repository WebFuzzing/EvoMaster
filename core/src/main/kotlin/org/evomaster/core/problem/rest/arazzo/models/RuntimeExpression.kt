package org.evomaster.core.problem.rest.arazzo.models

sealed class RuntimeExpression {
    object Url : RuntimeExpression()
    object Method : RuntimeExpression()
    object StatusCode : RuntimeExpression()

    data class Request(val source: Source) : RuntimeExpression()
    data class Response(val source: Source) : RuntimeExpression()

    data class Inputs(val name: String) : RuntimeExpression()
    data class Outputs(val name: String) : RuntimeExpression()
    data class Steps(val name: String) : RuntimeExpression()
    data class Workflows(val name: String) : RuntimeExpression()
    data class SourceDescriptions(val name: String) : RuntimeExpression()
    data class Components(val name: String) : RuntimeExpression()
    data class ComponentParameters(val parameterName: String) : RuntimeExpression()
}
