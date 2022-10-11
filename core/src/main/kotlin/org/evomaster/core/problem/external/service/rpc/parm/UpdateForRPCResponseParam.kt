package org.evomaster.core.problem.external.service.rpc.parm

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.api.service.param.UpdateForParam
import org.evomaster.core.problem.external.service.param.ResponseParam
import org.evomaster.core.search.gene.Gene

class UpdateForRPCResponseParam(val responseParam: RPCResponseParam)
    : ResponseParam("updateForBodyParam", responseParam.responseType, responseParam.responseBody, responseParam.extraProperties), UpdateForParam {

    override fun copyContent(): Param {
        return UpdateForRPCResponseParam(responseParam.copy() as RPCResponseParam)
    }


    override fun seeGenes(): List<Gene> {
        return responseParam.seeGenes()
    }

    override fun isUpdatedParam(param: Param): Boolean {
        return param is RPCResponseParam
    }
}