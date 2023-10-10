package org.evomaster.core.problem.externalservice.rpc

import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.externalservice.ApiExternalServiceAction
import org.evomaster.core.problem.externalservice.rpc.parm.ClassResponseParam
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
     * types of input params for the RPC function (ie, [functionName]) if exists
     */
    val inputParamTypes : List<String>?,

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
     *
     * note that null represents that it accepts any request
     */
    val requestRuleIdentifier: String?,

    responseParam: ClassResponseParam,
    active : Boolean = false,
    used : Boolean = false
) : ApiExternalServiceAction(responseParam, active, used) {

    companion object{
        fun getRPCExternalServiceActionName(apiDto: MockRPCExternalServiceDto, index: Int) =
            getRPCExternalServiceActionName(apiDto.interfaceFullName, apiDto.functionName, apiDto.requestRules?.get(index), apiDto.responseFullTypesWithGeneric?.get(index)?:apiDto.responseTypes[index])



        fun getRPCExternalServiceActionName(interfaceName: String, functionName: String, requestRuleIdentifier: String?, responseClassType: String) = "$interfaceName$EXACTION_NAME_SEPARATOR$functionName$EXACTION_NAME_SEPARATOR${requestRuleIdentifier?:"ANY"}$EXACTION_NAME_SEPARATOR$responseClassType"
    }

    override fun getName(): String {
        return getRPCExternalServiceActionName(interfaceName, functionName, requestRuleIdentifier, (response as ClassResponseParam).className)
    }

    override fun seeTopGenes(): List<out Gene> {
        return response.genes
    }


    override fun copyContent(): RPCExternalServiceAction {
        return RPCExternalServiceAction(interfaceName, functionName, inputParamTypes?.toList(), descriptiveInfo, requestRuleIdentifier, response.copy() as ClassResponseParam, active, used)
    }

    /**
     * @return a copy of this and release its restricted request identifier
     */
    fun getUnrestrictedRPCExternalServiceAction(): RPCExternalServiceAction {
        return RPCExternalServiceAction(interfaceName, functionName, inputParamTypes?.toList(), descriptiveInfo, null, response.copy() as ClassResponseParam)
    }

    fun addUpdateForParam(param: Param){
        addChild(param)
    }
}
