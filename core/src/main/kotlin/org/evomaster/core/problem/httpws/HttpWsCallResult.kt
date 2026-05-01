package org.evomaster.core.problem.httpws

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.problem.rest.data.HttpVerb
import javax.ws.rs.core.MediaType

abstract class HttpWsCallResult : EnterpriseActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId,stopping)

    @VisibleForTesting
    internal constructor(other: HttpWsCallResult) : super(other)

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
        const val INVALID_HTTP = "INVALID_HTTP"
        const val APPLIED_LINK = "APPLIED_LINK"
        const val LOCATION = "LOCATION"
        const val ALLOW = "ALLOW"
        const val RESPONSE_TIME_MS = "RESPONSE_TIME_MS"

        const val VULNERABLE_SSRF = "VULNERABLE_SSRF"
        const val VULNERABLE_SQLI = "VULNERABLE_SQLI"


        const val FLAKY_STATUS_CODE = "FLAKY_STATUS_CODE"
        const val FLAKY_BODY = "FLAKY_BODY"
        const val FLAKY_BODY_TYPE = "FLAKY_BODY_TYPE"
        const val FLAKY_ERROR_MESSAGE = "FLAKY_ERROR_MESSAGE"
    }

    /**
     * In some cases (eg infinite loop redirection), a HTTP call
     * might fail, and, as such, we might not have an actual "result"
     * object with info
     */
    fun failedCall(): Boolean{
        return getInfiniteLoop() || getTimedout() || getTcpProblem() || getInvalidHTTP()
    }


    /**
     * This is VERY TRICKY.
     * We can have a failedCall()... as well as a call that works fine in EM,
     * but then "fail" in the generated tests.
     * This depends on how different libraries dealing with HTTP handle invalid HTTP messages.
     * For example, Jersey throw an exception if below 100, but works fine if 600 or above.
     * RestAssured throws exceptions even in this latter case.
     * No idea (not checked) what the libraries in JS and Python do.
     * So, here we mark as "invalid" (equivalent to "failed") even successful calls, as long
     * as their status is invalid.
     *
     * To make things even more than the shitshow it is already, technically speaking the range 600-999 is not
     * invalid according to RFC specs... (it should be just treated as 5xx)
     */
    fun invalidCall(): Boolean{
        return failedCall() || invalidStatus()
    }

    fun invalidStatus() : Boolean{
        val statusCode = getStatusCode()
        if(statusCode != null && statusCode !in 100..<600){
            return true
        }
        //in theory, could be other cases that leads to an invalid HTTP besides the status
        return getInvalidHTTP()
    }

    fun setStatusCode(code: Int) {
        // We can't have this check here... it would crash if API sends a wrong code,
        // which now we actually do in some E2E tests...
//        if (code !in 100..<600) {
//            throw IllegalArgumentException("Invalid HTTP code $code")
//        }

        addResultValue(STATUS_CODE, code.toString())
    }

    fun getStatusCode(): Int? = getResultValue(STATUS_CODE)?.toInt()

    fun setLocation(location: String?) {
        if (location != null) {
            addResultValue(LOCATION, location)
        }
    }

    fun getLocation(): String? = getResultValue(LOCATION)

    fun setAllow(allow: String?){
        if(allow != null) {
            addResultValue(ALLOW, allow)
        }
    }

    fun getAllow(): String? = getResultValue(ALLOW)

    /**
     * Return verbs based on "allow" header.
     * It can return null to indicate there was no allow header.
     */
    fun getAllowedVerbs() : Set<HttpVerb>?{
        val allow = getAllow() ?: return null
        val fromAllow = allow.split(",")
            .mapNotNull {
                try{
                    HttpVerb.valueOf(it.trim())
                } catch (e: IllegalArgumentException){
                    //a bug, but we do not check this here
                    null
                }
            }
        return fromAllow.toSet()
    }

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

    fun setInvalidHTTP(invalidHTTP: Boolean) = addResultValue(INVALID_HTTP, invalidHTTP.toString())
    fun getInvalidHTTP() : Boolean = getResultValue(INVALID_HTTP)?.toBoolean() ?: false


    fun setAppliedLink(applied: Boolean) = addResultValue(APPLIED_LINK, applied.toString())
    fun getAppliedLink(): Boolean = getResultValue(APPLIED_LINK)?.toBoolean() ?: false

    /**
     * TODO: When dealing with additional vulnerabilities, consider changing the data structure.
     */
    fun setVulnerableForSSRF(on: Boolean) = addResultValue(VULNERABLE_SSRF, on.toString())
    fun getVulnerableForSSRF() : Boolean = getResultValue(VULNERABLE_SSRF)?.toBoolean() ?: false

    fun setVulnerableForSQLI(on: Boolean) = addResultValue(VULNERABLE_SQLI, on.toString())
    fun getVulnerableForSQLI() : Boolean = getResultValue(VULNERABLE_SQLI)?.toBoolean() ?: false

    fun setResponseTimeMs(responseTime: Long) = addResultValue(RESPONSE_TIME_MS, responseTime.toString())
    fun getResponseTimeMs(): Long? = getResultValue(RESPONSE_TIME_MS)?.toLong()


    fun setFlakyErrorMessage(msg: String)  = addResultValue(FLAKY_ERROR_MESSAGE, msg)
    fun getFlakyErrorMessage() : String? = getResultValue(FLAKY_ERROR_MESSAGE)

    fun setFlakyStatusCode(code: Int) = addResultValue(FLAKY_STATUS_CODE, code.toString())
    fun getFlakyStatusCode() : Int? = getResultValue(FLAKY_STATUS_CODE)?.toInt()

    fun setFlakyBody(body: String) = addResultValue(FLAKY_BODY, body)
    fun getFlakyBody() : String? = getResultValue(FLAKY_BODY)

    fun setFlakyBodyType(type: MediaType) = addResultValue(FLAKY_BODY_TYPE, type.toString())
    fun getFlakyBodyType() : MediaType? = getResultValue(FLAKY_BODY_TYPE)?.let { MediaType.valueOf(it) }

    fun setFlakiness(previous: HttpWsCallResult){
        val pStatusCode = previous.getStatusCode()
        if (pStatusCode != null && pStatusCode != getStatusCode()) {
            setFlakyStatusCode(pStatusCode)
        }

        val pBody = previous.getBody()
        if (pBody != null && pBody != getBody()) {
            setFlakyBody(pBody)
        }

        val pBodyType = previous.getBodyType()
        if (pBodyType != null && pBodyType != getBodyType()) {
            setFlakyBodyType(pBodyType)
        }

        val pMessage = previous.getErrorMessage()
        if (pMessage != null && pMessage != getErrorMessage()) {
            setFlakyErrorMessage(pMessage)
        }
    }
}
