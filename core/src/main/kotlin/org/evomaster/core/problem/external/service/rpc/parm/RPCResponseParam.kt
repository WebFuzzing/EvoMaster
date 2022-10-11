package org.evomaster.core.problem.external.service.rpc.parm

import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.OptionalGene

/**
 * need to consider the exception?
 */
class RPCResponseParam(
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

    override fun copyContent(): ResponseParam {
        return RPCResponseParam(className, responseType.copy() as EnumGene<String>, responseBody.copy() as OptionalGene).also { it.fromClass = this.fromClass }
    }

}