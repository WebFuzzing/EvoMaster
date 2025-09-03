package org.evomaster.core.problem.security.data

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
