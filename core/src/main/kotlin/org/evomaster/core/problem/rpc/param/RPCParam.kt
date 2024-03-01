package org.evomaster.core.problem.rpc.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

/**
 * params in requests
 */
class RPCParam (name: String, gene: Gene): Param(name, gene){

    override fun copyContent(): RPCParam {
        return RPCParam(name, gene.copy())
    }
}