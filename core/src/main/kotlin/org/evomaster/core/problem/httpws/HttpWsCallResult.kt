package org.evomaster.core.problem.httpws

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.search.action.ActionResult
import javax.ws.rs.core.MediaType

abstract class HttpWsCallResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId,stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    companion object {
        const val STATUS_CODE = "STATUS_CODE"
        const val BODY = "BODY"
        const val BODY_TYPE = "BODY_TYPE"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val TOO_LARGE_BODY = "TOO_LARGE_BODY"
        const val INFINITE_LOOP = "INFINITE_LOOP"
        const val TIMEDOUT = "TIMEDOUT"
        const val LAST_STATEMENT_WHEN_500 = "LAST_STATEMENT_WHEN_500"
        const val TCP_PROBLEM = "TCP_PROBLEM"
    }

    /**
     * In some cases (eg infinite loop redirection), a HTTP call
     * might fail, and, as such, we might not have an actual "result"
     * object with info
     */
    fun failedCall(): Boolean{
        return getInfiniteLoop() || getTimedout() || getTcpProblem()
    }



    fun setStatusCode(code: Int) {
        if (code < 100 || code >= 600) {
            throw IllegalArgumentException("Invalid HTTP code $code")
        }

        addResultValue(STATUS_CODE, code.toString())
    }

    fun getStatusCode(): Int? = getResultValue(STATUS_CODE)?.toInt()

    fun hasErrorCode() : Boolean = getStatusCode()!=null && getStatusCode()!! >= 500

    /*
        FIXME should rather be a byte[]
     */
    fun setBody(body: String){

        /*
            Try to infer if there is any error message in the body response.
            TODO this is very limited and adhoc. Should be refactored/extended with
            more heuristics.
         */
        var errorMsg : String? = null;
        val bodyType = getBodyType()
        if(getStatusCode() == 500
            && (bodyType != null && bodyType.isCompatible(MediaType.APPLICATION_JSON_TYPE))) {
            errorMsg = try {
                Gson().fromJson(body, Map::class.java)?.get("message").toString() ?: ""
            } catch (e: JsonSyntaxException) {
                null
            }
        }
        if(errorMsg != null){
            addResultValue(ERROR_MESSAGE, errorMsg)
        }

        return addResultValue(BODY, body)
    }

    fun getBody(): String? = getResultValue(BODY)

    fun getErrorMsg() : String? = getResultValue(ERROR_MESSAGE)

    fun setBodyType(bodyType: MediaType) = addResultValue(BODY_TYPE, bodyType.toString())
    fun getBodyType(): MediaType? {
        return getResultValue(BODY_TYPE)?.let { MediaType.valueOf(it) }
    }

    fun setTooLargeBody(on: Boolean) = addResultValue(TOO_LARGE_BODY, on.toString())
    fun getTooLargeBody(): Boolean = getResultValue(TOO_LARGE_BODY)?.toBoolean() ?: false


    fun setInfiniteLoop(on: Boolean) = addResultValue(INFINITE_LOOP, on.toString())
    fun getInfiniteLoop(): Boolean = getResultValue(INFINITE_LOOP)?.toBoolean() ?: false

    fun setTimedout(timedout: Boolean) = addResultValue(TIMEDOUT, timedout.toString())
    fun getTimedout(): Boolean = getResultValue(TIMEDOUT)?.toBoolean() ?: false

    fun setLastStatementWhen500(statement: String) {
        if(getStatusCode() != 500){
            throw IllegalArgumentException("Can set last statement only if status code is registered as 500. " +
                    "Current is " + getStatusCode())
        }
        addResultValue(LAST_STATEMENT_WHEN_500, statement)
    }
    fun getLastStatementWhen500() : String? = getResultValue(LAST_STATEMENT_WHEN_500)

    fun setTcpProblem(tcpProblem: Boolean) = addResultValue(TCP_PROBLEM, tcpProblem.toString())
    fun getTcpProblem() : Boolean = getResultValue(TCP_PROBLEM)?.toBoolean() ?: false
}