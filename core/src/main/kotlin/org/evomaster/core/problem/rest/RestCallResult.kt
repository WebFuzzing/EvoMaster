package org.evomaster.core.problem.rest

import org.evomaster.core.search.ActionResult
import javax.ws.rs.core.MediaType


class RestCallResult : ActionResult {

    constructor() : super()
    private constructor(other: ActionResult) : super(other)

    private val STATUS_CODE = "STATUS_CODE"
    private val BODY = "BODY"
    private val BODY_TYPE = "BODY_TYPE"

    override fun copy() : ActionResult{
        return RestCallResult(this)
    }


    fun setStatusCode(code: Int) {
        if (code < 100 || code >= 600) {
            throw IllegalArgumentException("Invalid HTTP code $code")
        }

        addResultValue(STATUS_CODE, code.toString())
    }

    fun getStatusCode() : Int? = getResultValue(STATUS_CODE)?.toInt()


    fun setBody(body: String) = addResultValue(BODY, body)
    fun getBody() : String? = getResultValue(BODY)

    fun setBodyType(bodyType: MediaType) = addResultValue(BODY_TYPE, bodyType.toString())
    fun getBodyType() : MediaType? {
        val res = getResultValue(BODY_TYPE)
        if(res != null){
            return MediaType.valueOf(res)
        } else{
            return null
        }
    }
}