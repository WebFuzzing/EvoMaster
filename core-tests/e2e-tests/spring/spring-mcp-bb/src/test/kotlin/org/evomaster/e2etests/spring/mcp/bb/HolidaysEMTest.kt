package org.evomaster.e2etests.spring.mcp.bb

import com.foo.mcp.bb.examples.spring.holidays.HolidaysController
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.JsonConfig
import io.restassured.config.RedirectConfig.redirectConfig
import io.restassured.http.ContentType
import io.restassured.path.json.config.JsonPathConfig
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Map
import java.util.UUID
import java.util.concurrent.TimeUnit

class HolidaysEMTest: EnterpriseTestBase() {

    companion object {


        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HolidaysController())
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
            RestAssured.useRelaxedHTTPSValidation()
            RestAssured.urlEncodingEnabled = false
            RestAssured.config = RestAssured.config()
                .jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))
                .redirect(redirectConfig().followRedirects(false))
        }

    }

    // ─── initialize ───────────────────────────────────────────────────────────
    @Test
    @Throws(Exception::class)
    fun initialize_returns_protocol_version_and_server_info() {
        val body = rpc(
            1, "initialize", Map.of<String?, Any?>(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of<String?, String?>("name", "test-client", "version", "1.0"),
                "capabilities", Map.of<Any?, Any?>()
            )
        )

        val uuid = UUID.randomUUID().toString()

        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(body)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", containsString(uuid))
            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
            .body("'result'.'server-info'.'name'", containsString("my-server"))
            .body("'result'.'server-info'.'version'", containsString("1.0"))

//        { "jsonrpc": "2.0", "id": 1, "result": {
//    "protocolVersion": ,
//    "capabilities": { "tools": {}, "resources": {}, "prompts": {} },
//    "serverInfo": { "name": "my-server", "version": "1.0" }
//}}
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private fun rpc(id: Int, method: String?, params: MutableMap<String?, Any?>?): MutableMap<String?, Any?> {
        val req: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        req.put("jsonrpc", "2.0")
        req.put("id", id)
        req.put("method", method)
        if (params != null) req.put("params", params)
        return req
    }

    /** Builds a JSON-RPC 2.0 notification (no id).  */
    private fun notification(method: String?): MutableMap<String?, Any?> {
        val req: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        req.put("jsonrpc", "2.0")
        req.put("method", method)
        return req
    }

}
