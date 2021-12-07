package org.evomaster.core.problem.rpc

import com.google.common.annotations.VisibleForTesting
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

/**
 * here, RPCCallResult is inherent from HttpWs for the moment
 * since some RPC might be based on HTTP, eg gRPC,
 * then we could reuse properties of HTTP results
 */
class RPCCallResult : ActionResult {

    companion object {
        const val LAST_STATEMENT_WHEN_P_BUG = "LAST_STATEMENT_WHEN_P_BUG"
        const val INVOCATION_CODE = "INVOCATION_CODE"
        const val CUSTOM_EXP_BODY = "CUSTOM_EXP_BODY"

        const val POTENTIAL_BUG_EXP_CODE = 500
        const val SUCCESS_CODE = 200
        const val CUSTOM_EXP_CODE = 100

        const val FAILED_CODE = 0
    }

    constructor(stopping: Boolean = false) : super(stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return RPCCallResult(this)
    }

    fun setFailedCall(){
        addResultValue(INVOCATION_CODE, FAILED_CODE.toString())
    }

    fun failedCall(): Boolean{
        return getInvocationCode() == FAILED_CODE
    }

    fun setSuccess(){
        addResultValue(INVOCATION_CODE, SUCCESS_CODE.toString())
    }

    fun getInvocationCode(): Int?{
        return getResultValue(INVOCATION_CODE)?.toInt()
    }

    fun setLastStatementForPotentialBug(info: String){
        addResultValue(LAST_STATEMENT_WHEN_P_BUG, info)
    }

    fun getLastStatementForPotentialBug() = getResultValue(LAST_STATEMENT_WHEN_P_BUG)

    fun setRPCException(dto: RPCExceptionInfoDto) {

        if (dto.type != null){
            val code = when(dto.type){
                RPCExceptionType.APP_INTERNAL_ERROR -> POTENTIAL_BUG_EXP_CODE
                RPCExceptionType.CUSTOMIZED_EXCEPTION-> CUSTOM_EXP_CODE
                else -> 400
            }

            addResultValue(dto.type.name, code.toString())
        }
    }

    fun setCustomizedExceptionBody(json: String){
        addResultValue(CUSTOM_EXP_BODY, json)
    }

    fun getCustomizedExceptionBody() = getResultValue(CUSTOM_EXP_BODY)

    override fun matchedType(action: Action): Boolean {
        return action is RPCCallAction
    }
}