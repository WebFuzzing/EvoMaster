package org.evomaster.core.problem.rpc

import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.problem.rpc.param.RPCInputParam
import org.evomaster.core.search.gene.Gene

/**
 * created by manzhang on 2021/11/25
 */
class RPCCallAction(
        val id: String,
        parameters: MutableList<Param>,
        /**
         * TODO check auth here
         */
        auth: AuthenticationInfo = NoAuth(),
) : HttpWsAction(auth, parameters)  {

    override fun getName(): String {
        return id
    }

    override fun seeGenes(): List<out Gene> {
        // ignore genes in response here
        return parameters.filterIsInstance<RPCInputParam>().flatMap { it.seeGenes() }
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): RPCCallAction {
        val p = parameters.asSequence().map(Param::copyContent).toMutableList()
        return RPCCallAction(id, p, auth)
    }
}