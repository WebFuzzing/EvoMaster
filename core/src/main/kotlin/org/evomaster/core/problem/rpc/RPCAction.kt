package org.evomaster.core.problem.rpc

import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

/**
 * created by manzhang on 2021/11/25
 */
class RPCAction(
        val id: String,
        parameters: MutableList<Param>,
        /**
         * TODO check auth here
         */
        auth: AuthenticationInfo = NoAuth(),
) : HttpWsAction(auth, parameters)  {
    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun seeGenes(): List<out Gene> {
        TODO("Not yet implemented")
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        TODO("Not yet implemented")
    }

    override fun copyContent(): StructuralElement {
        TODO("Not yet implemented")
    }
}