package org.evomaster.core.problem.rpc

import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.rpc.auth.RPCAuthenticationInfo
import org.evomaster.core.problem.rpc.auth.RPCNoAuth
import org.evomaster.core.problem.rpc.param.RPCParam

class AuthorizedRPCCallAction(
    id: String,
    inputParameters: MutableList<Param>,
    responseTemplate: RPCParam?,
    response : RPCParam?,
    auth: RPCAuthenticationInfo = RPCNoAuth(),
    var requiredAuth: RPCAuthenticationInfo = RPCNoAuth()

) : RPCCallAction(id, inputParameters, responseTemplate, response, auth){

    override fun copyContent(): RPCCallAction {
        val p = parameters.asSequence().map(Param::copyContent).toMutableList()
        return AuthorizedRPCCallAction(id, p, responseTemplate?.copyContent(), response?.copyContent(), auth, requiredAuth)
    }

    override fun setNoAuth() {
        super.setNoAuth()
        requiredAuth = RPCNoAuth()
    }
}