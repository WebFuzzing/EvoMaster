package org.evomaster.core.problem.rest

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.evomaster.core.search.ActionResult
import javax.ws.rs.core.MediaType


class RestCallResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)
    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)

    companion object {
        const val STATUS_CODE = "STATUS_CODE"
        const val BODY = "BODY"
        const val BODY_TYPE = "BODY_TYPE"
        const val TOO_LARGE_BODY = "TOO_LARGE_BODY"
        const val INFINITE_LOOP = "INFINITE_LOOP"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val HEURISTICS_FOR_CHAINED_LOCATION = "HEURISTICS_FOR_CHAINED_LOCATION"
        const val TIMEDOUT = "TIMEDOUT"
        const val LAST_STATEMENT_WHEN_500 = "LAST_STATEMENT_WHEN_500"
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
       return getInfiniteLoop() || getTimedout()
    }


    fun getResourceIdName() = "id"

    fun getResourceId(): String? {

        if(!MediaType.APPLICATION_JSON_TYPE.isCompatible(getBodyType())){
            //TODO could also handle other media types
            return null
        }

        return getBody()?.let {
            try {
                /*
                    TODO: "id" is the most common word, but could check
                    if others are used as well.
                 */
                Gson().fromJson(it, JsonObject::class.java).get(getResourceIdName())?.asString
            } catch (e: Exception){
                //nothing to do
                null
            }
        }
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
    fun setBody(body: String) = addResultValue(BODY, body)
    fun getBody(): String? = getResultValue(BODY)

    fun setBodyType(bodyType: MediaType) = addResultValue(BODY_TYPE, bodyType.toString())
    fun getBodyType(): MediaType? {
        return getResultValue(BODY_TYPE)?.let { MediaType.valueOf(it) }
    }

    fun setTooLargeBody(on: Boolean) = addResultValue(TOO_LARGE_BODY, on.toString())
    fun getTooLargeBody(): Boolean = getResultValue(TOO_LARGE_BODY)?.toBoolean() ?: false


    fun setInfiniteLoop(on: Boolean) = addResultValue(INFINITE_LOOP, on.toString())
    fun getInfiniteLoop(): Boolean = getResultValue(INFINITE_LOOP)?.toBoolean() ?: false

    fun setErrorMessage(msg: String) = addResultValue(ERROR_MESSAGE, msg)
    fun getErrorMessage(): String? = getResultValue(ERROR_MESSAGE)

    /**
     * It might happen that we need to chain call location, but
     * there was no "location" header in the response of call that
     * created a new resource. However, it "might" still be possible
     * to try to infer the location (based on object response and
     * the other available endpoints in the API)
     */
    fun setHeuristicsForChainedLocation(on: Boolean) = addResultValue(HEURISTICS_FOR_CHAINED_LOCATION, on.toString())
    fun getHeuristicsForChainedLocation(): Boolean = getResultValue(HEURISTICS_FOR_CHAINED_LOCATION)?.toBoolean() ?: false

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
}
