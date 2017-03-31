package org.evomaster.core.problem.rest

import org.evomaster.core.search.ActionResult
import javax.ws.rs.core.MediaType


class RestCallResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)
    private constructor(other: ActionResult) : super(other)

    companion object {
        val STATUS_CODE = "STATUS_CODE"
        val BODY = "BODY"
        val BODY_TYPE = "BODY_TYPE"
        val INFINITE_LOOP = "INFINITE_LOOP"
        val ERROR_MESSAGE = "ERROR_MESSAGE"
    }


    override fun copy(): ActionResult {
        return RestCallResult(this)
    }

    /**
     * In some cases (eg infinite loop redirection), a HTTP call
     * might fail, and, as such, we might not have an actual "result"
     * object with info
     */
    fun failedCall(): Boolean{
       return getInfiniteLoop()
    }

    fun setStatusCode(code: Int) {
        if (code < 100 || code >= 600) {
            throw IllegalArgumentException("Invalid HTTP code $code")
        }

        addResultValue(STATUS_CODE, code.toString())
    }

    fun getStatusCode(): Int? = getResultValue(STATUS_CODE)?.toInt()

    fun hasErrorCode() : Boolean = getStatusCode()!=null && getStatusCode()!! >= 500

    fun setBody(body: String) = addResultValue(BODY, body)
    fun getBody(): String? = getResultValue(BODY)

    fun setBodyType(bodyType: MediaType) = addResultValue(BODY_TYPE, bodyType.toString())
    fun getBodyType(): MediaType? {
        val res = getResultValue(BODY_TYPE)
        if (res != null) {
            return MediaType.valueOf(res)
        } else {
            return null
        }
    }

    fun setInfiniteLoop(on: Boolean) = addResultValue(INFINITE_LOOP, on.toString())
    fun getInfiniteLoop(): Boolean = getResultValue(INFINITE_LOOP)?.toBoolean() ?: false

    fun setErrorMessage(msg: String) = addResultValue(ERROR_MESSAGE, msg)
    fun getErrorMessage(): String? = getResultValue(ERROR_MESSAGE)
}