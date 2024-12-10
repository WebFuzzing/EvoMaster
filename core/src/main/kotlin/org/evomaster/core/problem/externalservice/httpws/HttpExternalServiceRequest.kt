package org.evomaster.core.problem.externalservice.httpws

import org.evomaster.core.problem.util.ParserDtoUtil.getJsonNodeFromText
import java.net.URL
import java.util.UUID

/**
 * Represent an external service call made to a WireMock
 * instance from a SUT.
 *
 * TODO: Properties have to extended further based on the need
 */
class HttpExternalServiceRequest(
    /**
     * Most likely the UUID created by the respective WireMock
     * while capturing the request.
     */
    val id: UUID,
    val method: String,
    val url: String,
    val absoluteURL: String,
    val wasMatched: Boolean,
    /**
     * refers to the WireMock instance which received the request.
     */
    val wireMockSignature: String,

    val actualAbsoluteURL : String,

    val headers : Map<String, String>,

    /**
     * body payload
     */
    val body : String?
) {

    companion object{
        const val REQUEST_DESCRIPTION_SEPARATOR = "::"


        fun getTextualURLFromRequestDescription(requestDescription: String): String {
            val components = requestDescription.split(REQUEST_DESCRIPTION_SEPARATOR)
            return components[1]
        }

        fun getTextualMethodURLFromRequestDescription(requestDescription: String): String {
            val components = requestDescription.split(REQUEST_DESCRIPTION_SEPARATOR)
            return "${components[0]}$REQUEST_DESCRIPTION_SEPARATOR${components[1]}"
        }

        private fun generateHttpRequestDescription(httpExternalServiceRequest: HttpExternalServiceRequest): String{
            return httpExternalServiceRequest.run {
                "$method$REQUEST_DESCRIPTION_SEPARATOR$absoluteURL$REQUEST_DESCRIPTION_SEPARATOR[${headers.keys.joinToString(";") { "$it:${headers[it]}" }}]$REQUEST_DESCRIPTION_SEPARATOR{${body?:"none"}}"
            }
        }
    }
    fun getId() : String {
        return id.toString()
    }

    /**
     * get description of this an HTTP request to external service
     */
    fun getDescription() = generateHttpRequestDescription(this)

    fun getContentType() : String?{
        if (body == null) return null

        val hct = headers.filterKeys { it.equals("contentType", ignoreCase = true) }
        if (hct.isNotEmpty()){
            return hct.values.first()
        }

        val json = getJsonNodeFromText(body) != null
        if (json)
            return "application/json"

        // TODO might add other derived types

        return null
    }

    fun getHostName() : String{
        return URL(absoluteURL).host
    }
}