package org.evomaster.core.problem.rpc.param

import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.search.gene.Gene

/**
 * params in requests
 */
class RPCInputParam (name: String, gene: Gene): Param(name, gene){

    override fun copyContent(): Param {
        return RPCInputParam(name, gene.copyContent())
    }
}