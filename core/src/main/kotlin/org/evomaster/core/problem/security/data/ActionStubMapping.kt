package org.evomaster.core.problem.security.data

/**
 * [ActionStubMapping] represents the information about [Action] name, WireMock [stub],
 * [port], and the full [url] for the callback link.
 */
class ActionStubMapping (
    val id: Long,
    val actionName: String,
    val stub: String,
    val port: Int,
    val url: String
) {

    fun getVerifierName(): String {
        return "httpCallbackVerifier${id}"
    }
}
