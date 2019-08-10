package org.evomaster.core.problem.grapqhl

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.evomaster.core.search.ActionResult

class GraphqlCallResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)
    private constructor(other: ActionResult) : super(other)

    companion object {
        val BODY = "BODY"
        val TOO_LARGE_BODY = "TOO_LARGE_BODY"
        val INFINITE_LOOP = "INFINITE_LOOP"
        val ERROR_MESSAGE = "ERROR_MESSAGE"
        val HEURISTICS_FOR_CHAINED_LOCATION = "HEURISTICS_FOR_CHAINED_LOCATION"
        val TIMEDOUT = "TIMEDOUT"
    }

    override fun copy(): ActionResult {
        return GraphqlCallResult(this)
    }

    /**
     * In some cases (eg infinite loop redirection), a HTTP call
     * might fail, and, as such, we might not have an actual "result"
     * object with info
     */
    fun failedCall(): Boolean{
        return getTimedout()
    }

    fun getResourceIdName() = "id"

    fun getResourceId(): String? {
        val bodyData = Gson().fromJson(getBody(), JsonObject::class.java).get("data").asJsonObject
        val resource = bodyData.entrySet()
        return resource.iterator().next().value.asJsonObject.get(getResourceIdName()).asString
    }


    fun setTimedout(timedout: Boolean) = addResultValue(TIMEDOUT, timedout.toString())
    fun getTimedout(): Boolean = getResultValue(TIMEDOUT)?.toBoolean() ?: false

    /*
    FIXME should rather be a byte[]
    */
    fun setBody(body: String) = addResultValue(BODY, body)
    fun getBody(): String? = getResultValue(BODY)

    fun setTooLargeBody(on: Boolean) = addResultValue(TOO_LARGE_BODY, on.toString())
    fun getTooLargeBody(): Boolean = getResultValue(TOO_LARGE_BODY)?.toBoolean() ?: false

    fun setInfiniteLoop(on: Boolean) = addResultValue(INFINITE_LOOP, on.toString())
    fun getInfiniteLoop(): Boolean = getResultValue(INFINITE_LOOP)?.toBoolean() ?: false

    /*
    error messages can be found as 'error' property in reponse json object
     */
    fun setErrorMessage(msg: String) = addResultValue(ERROR_MESSAGE, msg)
    fun getErrorMessage(): String? = getResultValue(ERROR_MESSAGE)


    /*
    TODO it needs to be refactored.
     */
    fun setHeuristicsForChainedLocation(on: Boolean) = addResultValue(HEURISTICS_FOR_CHAINED_LOCATION, on.toString())
    fun getHeuristicsForChainedLocation(): Boolean = getResultValue(HEURISTICS_FOR_CHAINED_LOCATION)?.toBoolean() ?: false

}