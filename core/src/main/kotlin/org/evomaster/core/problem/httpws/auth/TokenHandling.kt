package org.evomaster.core.problem.httpws.auth

class TokenHandling (

    val extractFrom : ExtractFrom,

    /**
     * How to extract the token from the HTTP response.
     * This depends on where the token is located.
     * For a 'body' location, the returned body payload like a JSON could have few fields, possibly nested.
     * In this case, this selector is expressed as a JSON Pointer (RFC 6901).
     * For a 'header' location, this selector would represent the name of the HTTP header (e.g., 'X-Auth-Token').
     */
    val extractSelector: String,

    val sendIn: SendIn,

    val sendName: String,

    /**
     * Template with {token} placeholder.
     * The placeholder will be interpolated with the actual token value.
     * When sending out the obtained token in an HTTP request, specify if there should be any other
     * text information around it.
     * For example, when sending the token in an 'Authorization' header, possible
     * values could be 'Bearer {token}' and 'JWT {token}'.
     */
    val sendTemplate: String
){

    enum class ExtractFrom { BODY, HEADER}

    enum class SendIn {HEADER, QUERY}

    companion object {

        const val TOKEN_INTERPOLATION_TEMPLATE = "{token}"
    }
}