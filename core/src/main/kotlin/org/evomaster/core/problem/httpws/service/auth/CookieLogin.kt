package org.evomaster.core.problem.httpws.service.auth

import org.evomaster.client.java.controller.api.dto.CookieLoginDto
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.HttpVerb
import java.net.URLEncoder

/**
 * Created by arcuri82 on 24-Oct-19.
 */
class CookieLogin(

        /**
         * The id of the user
         */
        val username: String,

        /**
         * The password of the user.
         * This must NOT be hashed.
         */
        val password: String,

        /**
         * The name of the field in the body payload containing the username
         */
        val usernameField: String,

        /**
         * The name of the field in the body payload containing the password
         */
        val passwordField: String,

        /**
         * The URL of the endpoint, e.g., "/login"
         */
        val loginEndpointUrl: String,

        /**
         * The HTTP verb used to send the data.
         * Usually a "POST".
         */
        val httpVerb: HttpVerb,

        /**
         * The encoding type used to specify how the data is sent
         */
        val contentType: ContentType


) {

    companion object {

        fun fromDto(dto: CookieLoginDto) = CookieLogin(
                dto.username,
                dto.password,
                dto.usernameField,
                dto.passwordField,
                dto.loginEndpointUrl,
                HttpVerb.valueOf(dto.httpVerb.toString()),
                ContentType.valueOf(dto.contentType.toString())
        )
    }

    private fun encoded(s: String) = URLEncoder.encode(s, "UTF-8")

    fun payload(): String {

        return when (contentType) {
            ContentType.X_WWW_FORM_URLENCODED ->
                "${encoded(usernameField)}=${encoded(username)}&${encoded(passwordField)}=${encoded(password)}"
            ContentType.JSON -> """
                {"$usernameField": "$username", "$passwordField": "$password"}
            """.trimIndent()
            else -> throw IllegalStateException("Currently not supporting $contentType for auth")
        }

    }

    /**
     * @return whether the specified [loginEndpointUrl] is a complete url, i.e., start with http/https protocol
     */
    fun isFullUrlSpecified() = loginEndpointUrl.startsWith("https://")|| loginEndpointUrl.startsWith("http://")

    /**
     * @return a complete URL based on [baseUrl] and [loginEndpointUrl]
     */
    fun getUrl(baseUrl: String) : String{
        if (isFullUrlSpecified()) return loginEndpointUrl

        return baseUrl.run { if (!endsWith("/")) "$this/" else this} + loginEndpointUrl.run { if (startsWith("/")) this.drop(1) else this }
    }
}