package org.evomaster.e2etests.spring.mcp.bb

import com.foo.mcp.bb.examples.spring.holidays.HolidaysController
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.JsonConfig
import io.restassured.config.RedirectConfig.redirectConfig
import io.restassured.path.json.config.JsonPathConfig
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Map
import java.util.UUID

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

    @Test
    @Throws(Exception::class)
    fun test_1_initialize_returns_protocol_version_and_server_info() {
        val id = 1
        val body = body(
            id, "initialize", Map.of<String?, Any?>(
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
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))
    }

    @Test
    @Throws(Exception::class)
    fun test_2_list_tools_returns_server_available_tools() {
        val id = 1
        val initializeBody = body(
            id, "initialize", Map.of<String?, Any?>(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of<String?, String?>("name", "test-client", "version", "1.0"),
                "capabilities", Map.of<Any?, Any?>()
            )
        )

        val uuid = UUID.randomUUID().toString()

        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(initializeBody)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))


        val listToolsBody = body(id, "tools/list", mutableMapOf())
        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(listToolsBody)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'tools'.size()", equalTo(4))
            .body("'result'.'tools'[0].'name'", containsString("list_destinations"))
            .body("'result'.'tools'[0].'description'", containsString("List all available holiday destinations with a short description."))
            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[1].'name'", containsString("get_destination_info"))
            .body("'result'.'tools'[1].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'type'", containsString("string"))
            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
            .body("'result'.'tools'[1].'inputSchema'.'required'.size()", equalTo(1))
            .body("'result'.'tools'[1].'inputSchema'.'required'[0]", containsString("destination"))
    }

    @Test
    @Throws(Exception::class)
    fun test_3_call_tool_with_destination() {
        val id = 1
        val initializeBody = body(
            id, "initialize", Map.of<String?, Any?>(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of<String?, String?>("name", "test-client", "version", "1.0"),
                "capabilities", Map.of<Any?, Any?>()
            )
        )

        val uuid = UUID.randomUUID().toString()

        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(initializeBody)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))


        val listToolsBody = body(id, "tools/list", mutableMapOf())
        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(listToolsBody)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'tools'.size()", equalTo(4))
            .body("'result'.'tools'[0].'name'", containsString("list_destinations"))
            .body("'result'.'tools'[0].'description'", containsString("List all available holiday destinations with a short description."))
            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[1].'name'", containsString("get_destination_info"))
            .body("'result'.'tools'[1].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'type'", containsString("string"))
            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
            .body("'result'.'tools'[1].'inputSchema'.'required'.size()", equalTo(1))
            .body("'result'.'tools'[1].'inputSchema'.'required'[0]", containsString("destination"))


        val callToolBody = body(id, "tools/call", Map.of<String?, Any?>(
            "name", "get_destination_info",
            "arguments", Map.of<String?, String?>("destination", "paris")
        ))
        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(callToolBody)
            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'content'.size()", equalTo(1))
            .body("'result'.'content'[0].'type'", containsString("text"))
            .body("'result'.'content'[0].'text'", containsString("The City of Light: world-class art, haute cuisine, grand boulevards, and the iconic Eiffel Tower."))
    }

    private fun body(id: Int, method: String?, params: MutableMap<String?, Any?>?): MutableMap<String?, Any?> {
        val req: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        req.put("jsonrpc", "2.0")
        req.put("id", id)
        req.put("method", method)
        if (params != null) req.put("params", params)
        return req
    }


}
