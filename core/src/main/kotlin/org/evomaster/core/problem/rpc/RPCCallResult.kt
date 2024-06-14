package org.evomaster.core.problem.rpc

import com.google.common.annotations.VisibleForTesting
import org.evomaster.client.java.controller.api.dto.CustomizedCallResultCode
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCExceptionInfoDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionCategory
import org.evomaster.client.java.controller.api.dto.problem.rpc.exception.RPCExceptionType
import org.evomaster.core.Lazy
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/**
 * define RPC call result with various situations,
 *  eg, success, exception, potential bug, fail (some problems when invoking the call, eg, timeout, network)
 */
class RPCCallResult : ActionResult {

    companion object {
        const val LAST_STATEMENT_WHEN_POTENTIAL_FAULT = "LAST_STATEMENT_WHEN_POTENTIAL_FAULT"
        const val INVOCATION_CODE = "INVOCATION_CODE"
        const val CUSTOM_EXP_BODY = "CUSTOM_EXP_BODY"
        const val EXCEPTION_CODE = "EXCEPTION_CODE"
        const val EXCEPTION_IMPORTANCE_LEVEL = "EXCEPTION_IMPORTANCE_LEVEL"
        const val EXCEPTION_TYPE_NAME = "EXCEPTION_TYPE_NAME"
        const val CUSTOM_BUSINESS_LOGIC_CODE = "CUSTOM_BUSINESS_LOGIC_CODE"
        const val CUSTOM_BUSINESS_LOGIC_SUCCESS = 200
        const val CUSTOM_BUSINESS_LOGIC_SERVICE_ERROR = 500
        const val CUSTOM_BUSINESS_LOGIC_OTHERWISE_ERROR = 400
        const val HANDLED_RESULTS = "HANDLED_RESULTS"
        const val HANDLED_RESULTS_NOT_NULL = "NOT_NULL"
        const val HANDLED_RESULTS_NULL = "NULL"
        const val HANDLED_COLLECTION_RESULTS = "HANDLED_COLLECTION_RESULTS"
        const val HANDLED_COLLECTION_RESULTS_ISEMPTY = "IS_EMPTY"
        const val HANDLED_COLLECTION_RESULTS_NOTEMPTY = "NOT_EMPTY"

        const val INVOCATION_SCRIPT= "INVOCATION_SCRIPT"
//        const val RESPONSE_JSON_VALUE= "RESPONSE_JSON_VALUE"
        const val RESPONSE_VARIABLE_NAME = "RESPONSE_VARIABLE_NAME"
        const val RESPONSE_ASSERTION_SCRIPT = "RESPONSE_ASSERTION_SCRIPT"
    }

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    override fun copy(): ActionResult {
        return RPCCallResult(this)
    }

    fun setResponseVariableName(name: String){
        addResultValue(RESPONSE_VARIABLE_NAME, name)
    }

    fun getResponseVariableName() = getResultValue(RESPONSE_VARIABLE_NAME)

//    fun setResponseJsonValue(jsonValue: String){
//        addResultValue(RESPONSE_JSON_VALUE, jsonValue)
//    }
//
//    fun getResponseJsonValue() = getResultValue(RESPONSE_JSON_VALUE)

    fun setTestScript(lines: List<String>){
        addResultValue(INVOCATION_SCRIPT, lines.joinToString(System.lineSeparator()))
    }

    fun getTestScript() = getResultValue(INVOCATION_SCRIPT)

    fun setAssertionScript(lines: List<String>){
        addResultValue(RESPONSE_ASSERTION_SCRIPT, lines.joinToString(System.lineSeparator()))
    }

    fun getAssertionScript() = getResultValue(RESPONSE_ASSERTION_SCRIPT)

    fun setHandledResponse(isNull : Boolean){
        addResultValue(HANDLED_RESULTS, if (isNull) HANDLED_RESULTS_NULL else HANDLED_RESULTS_NOT_NULL)
    }

    fun setHandledCollectionResponse(isEmpty: Boolean){
        addResultValue(HANDLED_COLLECTION_RESULTS, if (isEmpty) HANDLED_COLLECTION_RESULTS_ISEMPTY else HANDLED_COLLECTION_RESULTS_NOTEMPTY)
    }

    fun hasResponse() = getResultValue(HANDLED_RESULTS) != null
    fun hasCollectionResponse() = getResultValue(HANDLED_COLLECTION_RESULTS) != null

    fun isNotNullHandledResponse() = getResultValue(HANDLED_RESULTS) == HANDLED_RESULTS_NOT_NULL
    fun isNotEmptyHandledResponse() = getResultValue(HANDLED_COLLECTION_RESULTS) == HANDLED_COLLECTION_RESULTS_NOTEMPTY

    /**
     * RPC fails to be processed with error msg if it has
     */
    fun setFailedCall(msg: String? = null){
        addResultValue(INVOCATION_CODE, RPCCallResultCategory.FAILED.name)
        if (msg != null) setErrorMessage(msg)
    }

    fun failedCall(): Boolean{
        return getInvocationCode() != RPCCallResultCategory.HANDLED.name
    }

    fun setSuccess(){
        addResultValue(INVOCATION_CODE, RPCCallResultCategory.HANDLED.name)
    }

    fun getInvocationCode()= getResultValue(INVOCATION_CODE)

    fun getExceptionCode() = getResultValue(EXCEPTION_CODE)

    fun getExceptionImportanceLevel() : Int{
        val level = getResultValue(EXCEPTION_IMPORTANCE_LEVEL) ?: return -1
        return try {
            level.toInt()
        }catch (e: NumberFormatException){
            -1
        }

    }

    fun getExceptionTypeName() = getResultValue(EXCEPTION_TYPE_NAME)

    fun getExceptionInfo() : String{
        Lazy.assert { getInvocationCode() != null && getExceptionCode() != null && getExceptionTypeName() != null }
        return "${getInvocationCode()}:${getExceptionTypeName()}"
    }

    fun setLastStatementForInternalError(info: String){
        addResultValue(LAST_STATEMENT_WHEN_POTENTIAL_FAULT, info)
    }

    fun setCustomizedBusinessLogicCode(result: CustomizedCallResultCode){
        when(result){
            CustomizedCallResultCode.SUCCESS -> addResultValue(CUSTOM_BUSINESS_LOGIC_CODE, CUSTOM_BUSINESS_LOGIC_SUCCESS.toString())
            CustomizedCallResultCode.SERVICE_ERROR -> addResultValue(CUSTOM_BUSINESS_LOGIC_CODE, CUSTOM_BUSINESS_LOGIC_SERVICE_ERROR.toString())
            CustomizedCallResultCode.OTHER_ERROR -> addResultValue(CUSTOM_BUSINESS_LOGIC_CODE, CUSTOM_BUSINESS_LOGIC_OTHERWISE_ERROR.toString())
        }

    }

    fun isSuccessfulBusinessLogicCode() = getResultValue(CUSTOM_BUSINESS_LOGIC_CODE) == CUSTOM_BUSINESS_LOGIC_SUCCESS.toString()
    fun isCustomizedServiceError() = getResultValue(CUSTOM_BUSINESS_LOGIC_CODE) == CUSTOM_BUSINESS_LOGIC_SERVICE_ERROR.toString()
    fun isOtherwiseCustomizedServiceError() = getResultValue(CUSTOM_BUSINESS_LOGIC_CODE)  == CUSTOM_BUSINESS_LOGIC_OTHERWISE_ERROR.toString()

    fun getLastStatementForPotentialBug() = getResultValue(LAST_STATEMENT_WHEN_POTENTIAL_FAULT)

    fun setRPCException(dto: RPCExceptionInfoDto) {

        if (dto.type != null){
            val code = when(dto.type){
                RPCExceptionType.APP_INTERNAL_ERROR -> RPCCallResultCategory.INTERNAL_ERROR
                RPCExceptionType.UNEXPECTED_EXCEPTION -> RPCCallResultCategory.UNEXPECTED_EXCEPTION
                RPCExceptionType.CUSTOMIZED_EXCEPTION-> RPCCallResultCategory.CUSTOM_EXCEPTION
                else -> {
                    when(dto.type.category){
                        RPCExceptionCategory.PROTOCOL-> RPCCallResultCategory.PROTOCOL_ERROR
                        RPCExceptionCategory.TRANSPORT-> RPCCallResultCategory.TRANSPORT_ERROR
                        else-> RPCCallResultCategory.OTHERWISE_EXCEPTION
                    }
                }
            }

            addResultValue(EXCEPTION_CODE, dto.type.name)
            addResultValue(EXCEPTION_IMPORTANCE_LEVEL, "${dto.importanceLevel}")
            addResultValue(EXCEPTION_TYPE_NAME, dto.exceptionName)
            addResultValue(INVOCATION_CODE, code.name)

            if (dto.exceptionMessage != null){
                setErrorMessage(dto.exceptionMessage)
            }

        }
    }

    fun setCustomizedExceptionBody(json: String){
        addResultValue(CUSTOM_EXP_BODY, json)
    }

    fun getCustomizedExceptionBody() = getResultValue(CUSTOM_EXP_BODY)

    override fun matchedType(action: Action): Boolean {
        return action is RPCCallAction
    }

    fun hasPotentialFault() : Boolean = getInvocationCode() == RPCCallResultCategory.INTERNAL_ERROR.name || isExceptionThrown()

    fun isExceptionThrown() : Boolean = getExceptionCode() != null

}
