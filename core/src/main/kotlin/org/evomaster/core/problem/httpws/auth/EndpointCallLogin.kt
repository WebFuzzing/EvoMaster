package org.evomaster.core.problem.httpws.auth

import org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.HttpVerb
import java.net.URLEncoder

class EndpointCallLogin (

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
){

    init {
        if(endpoint == null && externalEndpointURL == null){
            throw IllegalArgumentException("Either 'endpoint' or 'externalEndpointURL' should be specified")
        }
        if(endpoint != null && externalEndpointURL != null){
            throw IllegalArgumentException("Cannot have both 'endpoint' and 'externalEndpointURL' specified. It is ambiguous.")
        }
    }


    fun fromDto(dto: LoginEndpointDto) = EndpointCallLogin(

    )

    fun getUrl(baseUrl: String) : String{
        if (isFullUrlSpecified()) return loginEndpointUrl

        return baseUrl.run { if (!endsWith("/")) "$this/" else this} + loginEndpointUrl.run { if (startsWith("/")) this.drop(1) else this }
    }

    private fun encoded(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun computePayload(): String {

        return when (contentType) {
            ContentType.X_WWW_FORM_URLENCODED ->
                "${encoded(usernameField)}=${encoded(username)}&${encoded(passwordField)}=${encoded(password)}"
            ContentType.JSON -> """
                {"$usernameField": "$username", "$passwordField": "$password"}
            """.trimIndent()
            else -> throw IllegalStateException("Currently not supporting $contentType for auth")
        }
    }
}