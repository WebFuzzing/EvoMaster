package org.evomaster.core.problem.external.service.rpc

import org.evomaster.core.problem.external.service.ApiExternalServiceAction
import org.evomaster.core.problem.external.service.rpc.parm.RPCResponseParam
import org.evomaster.core.search.gene.Gene

class RPCExternalServiceAction(
    /**
     * the interface name
     */
    val interfaceName: String,
    /**
     * the method name
     */
    val functionName: String,

    /**
     * descriptive info for the external service if needed
     * eg, app key for identifying the external service
     */
    val descriptiveInfo : String? = null,

    /**
     * response might be decided based on requests
     * such as x > 1 return A, otherwise return B (could exist in the seeded test)
     * this property provides an identifier for such rules (eg, x>1) if exists.
     * the rule is provided by the user (eg, with customization) and it is immutable.
     */
    val requestRuleIdentifier: String?,

    responseParam: RPCResponseParam,
    active : Boolean = false,
    used : Boolean = false,
    localId : String
) : ApiExternalServiceAction(responseParam, active, used, localId) {

    companion object{
        private const val RPC_EX_NAME_SEPARATOR =":::"

        fun getRPCExternalServiceActionName(interfaceName: String, functionName: String, requestRuleIdentifier: String?) = "$interfaceName$RPC_EX_NAME_SEPARATOR$functionName$RPC_EX_NAME_SEPARATOR${requestRuleIdentifier?:"ANY"}"
    }

    override fun getName(): String {
        return getRPCExternalServiceActionName(interfaceName, functionName, requestRuleIdentifier)
    }

    override fun seeTopGenes(): List<out Gene> {
        return response.genes
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return false
    }

    override fun copyContent(): RPCExternalServiceAction {
        return RPCExternalServiceAction(interfaceName, functionName, descriptiveInfo, requestRuleIdentifier, response.copy() as RPCResponseParam, active, used, getLocalId())
    }
}