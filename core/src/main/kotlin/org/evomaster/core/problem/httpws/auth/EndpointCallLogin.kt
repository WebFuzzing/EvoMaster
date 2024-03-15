package org.evomaster.core.problem.httpws.auth

import org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto
import org.evomaster.client.java.controller.api.dto.auth.PayloadUsernamePasswordDto
import org.evomaster.client.java.controller.api.dto.auth.TokenHandlingDto
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.HttpVerb
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

class EndpointCallLogin(

    /**
     * Unique identifier for this auth setting definition
     */
    val name: String,

    /**
     * The endpoint path (eg "/login") where to execute the login.
     * It assumes it is on same server of API.
     * If not, rather use externalEndpointURL
     */
    val endpoint: String?,

    /**
     * If the login endpoint is on a different server, here can rather specify the full URL for it.
     */
    val externalEndpointURL: String?,

    /**
     * The raw payload to send, as a string
     *
     * TODO should this be nullable? eg, what about case of login based on GET with query params?
     */
    val payload: String,

    /**
     * The verb used to connect to the login endpoint.
     * Most of the time, this will be a POST.
     */
    val verb: HttpVerb,

    /**
     * Specify the format in which the payload is sent to the login endpoint.
     * A common example is "application/json"
     */
    val contentType: ContentType,

    val token: TokenHandling? = null
) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name")
        }
        if (endpoint == null && externalEndpointURL == null) {
            throw IllegalArgumentException("Either 'endpoint' or 'externalEndpointURL' should be specified")
        }
        if (endpoint != null && externalEndpointURL != null) {
            throw IllegalArgumentException("Cannot have both 'endpoint' and 'externalEndpointURL' specified. It is ambiguous.")
        }
        if (endpoint != null && !endpoint.startsWith("/")) {
            throw IllegalArgumentException(
                "Login endpoint definition must start with a /. It is not a full URL." +
                        " For example: '/login'"
            )
        }
        if (externalEndpointURL != null) {
            try {
                URL(externalEndpointURL)
            } catch (e: MalformedURLException) {
                throw IllegalArgumentException("'externalEndpointURL' is not a valid URL: ${e.message}")
            }
        }
        if (payload.isEmpty()) {
            throw IllegalArgumentException("Empty payload")
        }
    }

    companion object {
        fun fromDto(name: String, dto: LoginEndpointDto) = EndpointCallLogin(
            name = name,
            endpoint = dto.endpoint,
            externalEndpointURL = dto.externalEndpointURL,
            payload = dto.payloadRaw ?: computePayload(
                dto.payloadUserPwd ?: throw IllegalArgumentException("Must specify a payload for auth info"),
                ContentType.from(dto.contentType)
            ),
            verb = HttpVerb.valueOf(dto.verb.toString()),
            contentType = ContentType.from(dto.contentType),
            token = if (dto.expectCookies!=null && dto.expectCookies) null else computeTokenHandling(dto.token)
        )

        private fun computeTokenHandling(dto: TokenHandlingDto) = TokenHandling(
            extractFromField = dto.extractFromField,
            httpHeaderName = dto.httpHeaderName,
            headerPrefix = dto.headerPrefix
        )


        private fun computePayload(dto: PayloadUsernamePasswordDto, contentType: ContentType): String {

            return when (contentType) {
                ContentType.X_WWW_FORM_URLENCODED ->
                    "${encoded(dto.usernameField)}=${encoded(dto.username)}&${encoded(dto.passwordField)}=${encoded(dto.password)}"

                ContentType.JSON -> """
                {"${dto.usernameField}": "${dto.username}", "${dto.passwordField}": "${dto.password}"}
            """.trimIndent()

                else -> throw IllegalStateException("Currently not supporting $contentType for auth")
            }
        }

        private fun encoded(s: String) = URLEncoder.encode(s, "UTF-8")
    }

    fun expectsCookie() = token == null

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