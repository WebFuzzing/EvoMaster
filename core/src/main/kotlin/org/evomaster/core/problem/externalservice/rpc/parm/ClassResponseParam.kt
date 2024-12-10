package org.evomaster.core.problem.externalservice.rpc.parm

import org.evomaster.core.problem.externalservice.param.ResponseParam
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene

/**
 *
 * response constrained with a class
 *
 * need to consider the exception?
 *
 */
class ClassResponseParam(
    val className: String,
    responseType: EnumGene<String>,
    response: OptionalGene
) : ResponseParam("return", responseType, response, listOf()) {

    /**
     * whether the response is parsed based on class (true) or json (false)
     */
    var fromClass = false
        private set

    /**
     * confirm that the response is parsed based on class
     */
    fun responseParsedWithClass() {
        fromClass = true
    }

    override fun copyContent(): ClassResponseParam {
        return ClassResponseParam(className, responseType.copy() as EnumGene<String>, responseBody.copy() as OptionalGene).also { it.fromClass = this.fromClass }
    }

    fun copyWithSpecifiedResponseBody(response: OptionalGene): ClassResponseParam {
        return ClassResponseParam(className, responseType.copy() as EnumGene<String>, response.copy() as OptionalGene).also { it.fromClass = this.fromClass }
    }

}
