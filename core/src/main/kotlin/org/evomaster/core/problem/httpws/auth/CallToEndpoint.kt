package org.evomaster.core.problem.httpws.auth

import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb
import java.net.MalformedURLException
import java.net.URL

class CallToEndpoint(
    /**
     * The endpoint path (eg "/v1/api/data") where to execute the call.
     * It assumes it is on same server of API.
     * If not, rather use externalEndpointURL
     */
    val endpoint: String?,

    /**
     * If the endpoint is on a different server, here can rather specify the full URL for it.
     */
    val externalEndpointURL: String?,

    /**
     * The raw payload to send, as a string, if any
     */
    val payload: String?,


    val headers: List<AuthenticationHeader>,

    /**
     * The verb used to make the call to the endpoint.
     * Most of the time, for auth this will be a POST.
     */
    val verb: HttpVerb,

    /**
     * Specify the format in which the payload is sent to the endpoint.
     * A common example is "application/json"
     */
    val contentType: ContentType?,
) {

    init{
        if (endpoint == null && externalEndpointURL == null) {
            throw IllegalArgumentException("Either 'endpoint' or 'externalEndpointURL' should be specified")
        }
        if (endpoint != null && externalEndpointURL != null) {
            throw IllegalArgumentException("Cannot have both 'endpoint' and 'externalEndpointURL' specified. It is ambiguous.")
        }
        if (endpoint != null && !endpoint.startsWith("/")) {
            throw IllegalArgumentException(
                "Endpoint definition must start with a /. It is not a full URL." +
                        " For example: '/login'"
            )
        }
        if (externalEndpointURL != null) {
            try {
                //FIXME should not use URL for validation, as Java URL is not standard compliant
                URL(externalEndpointURL)
            } catch (e: MalformedURLException) {
                throw IllegalArgumentException("'externalEndpointURL' is not a valid URL: ${e.message}")
            }
        }
        if( (payload != null && contentType==null) || (payload==null && contentType!=null)) {
            throw IllegalArgumentException("Payload and contentType must be both specified, or none specified")
        }
    }


    fun getUrl(baseUrl: String): String {
        val s = baseUrl.trim()
        if (externalEndpointURL != null) return externalEndpointURL

        if (!s.startsWith("http://", true) && !s.startsWith("https://")) {
            throw IllegalArgumentException("baseUrl should use HTTP(S): $baseUrl")
        }
        Lazy.assert { endpoint != null && endpoint.startsWith("/") }
        return if (s.endsWith("/")) {
            s.substring(0, s.length - 1) + endpoint
        } else {
            s + endpoint
        }
    }
}