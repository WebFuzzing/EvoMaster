package org.evomaster.core.problem.external.service.rpc.parm

import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.OptionalGene

/**
 * need to consider the exception?
 */
class RPCResponseParam(
    responseType: EnumGene<String>,
    response: OptionalGene
) : ResponseParam("return", responseType, response, listOf()) {
}