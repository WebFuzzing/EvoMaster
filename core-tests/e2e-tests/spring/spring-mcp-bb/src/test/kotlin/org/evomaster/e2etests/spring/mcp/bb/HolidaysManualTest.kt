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
import java.util.UUID

class HolidaysManualTest: EnterpriseTestBase() {

    companion object {

        @BeforeAll
        @JvmStatic
        fun init() {
            shouldApplyInstrumentation = false
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

        val initParams = mutableMapOf<String, Any>()
        initParams["protocolVersion"] = "2024-11-05"
        val clientInfo = mutableMapOf<String, String>()
        clientInfo["name"] = "test-client"
        clientInfo["version"] = "1.0"
        initParams["clientInfo"] = clientInfo
        initParams["capabilities"] = emptyMap<String, Any>()

        val body = body(id, "initialize", initParams)


        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(body)
            .post("${baseUrlOfSut}/mcp")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2025-06-18"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))
    }

    @Test
    @Throws(Exception::class)
    fun test_2_list_tools_returns_server_available_tools() {
        val id = 1

        val initParams = mutableMapOf<String, Any>()
        initParams["protocolVersion"] = "2024-11-05"
        val clientInfo = mutableMapOf<String, String>()
        clientInfo["name"] = "test-client"
        clientInfo["version"] = "1.0"
        initParams["clientInfo"] = clientInfo
        initParams["capabilities"] = emptyMap<String, Any>()

        val initializeBody = body(id, "initialize", initParams)

        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(initializeBody)
            .post("${baseUrlOfSut}/mcp")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2025-06-18"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))


        val listToolsBody = body(id, "tools/list", mutableMapOf())
        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(listToolsBody)
            .post("${baseUrlOfSut}/mcp")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'tools'.size()", equalTo(2))
            .body("'result'.'tools'[0].'name'", containsString("get_destination_info"))
            .body("'result'.'tools'[0].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[0].'inputSchema'.'properties'.'destinationId'.'type'", containsString("string"))
            .body("'result'.'tools'[0].'inputSchema'.'properties'.'destinationId'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
            .body("'result'.'tools'[0].'inputSchema'.'required'.size()", equalTo(1))
            .body("'result'.'tools'[0].'inputSchema'.'required'[0]", containsString("destinationId"))
            .body("'result'.'tools'[1].'name'", containsString("list_destinations"))
            .body("'result'.'tools'[1].'description'", containsString("List all available holiday destinations with a short description."))
            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))
    }

    @Test
    @Throws(Exception::class)
    fun test_3_call_tool_with_destination() {
        val id = 1

        val initParams = mutableMapOf<String, Any>()
        initParams["protocolVersion"] = "2024-11-05"
        val clientInfo = mutableMapOf<String, String>()
        clientInfo["name"] = "test-client"
        clientInfo["version"] = "1.0"
        initParams["clientInfo"] = clientInfo
        initParams["capabilities"] = emptyMap<String, Any>()

        val initializeBody = body(id, "initialize", initParams)

        val uuid = UUID.randomUUID().toString()

        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(initializeBody)
            .post("${baseUrlOfSut}/mcp")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'protocolVersion'", containsString("2025-06-18"))
            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))


        val listToolsBody = body(id, "tools/list", mutableMapOf())
        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(listToolsBody)
            .post("${baseUrlOfSut}/mcp")
            .then()
            .statusCode(200)
            .assertThat()
            .contentType("application/json")
            .body("'jsonrpc'", containsString("2.0"))
            .body("'id'", `is`(id))
            .body("'result'.'tools'.size()", equalTo(2))
            .body("'result'.'tools'[0].'name'", containsString("get_destination_info"))
            .body("'result'.'tools'[0].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
            .body("'result'.'tools'[0].'inputSchema'.'properties'.'destinationId'.'type'", containsString("string"))
            .body("'result'.'tools'[0].'inputSchema'.'properties'.'destinationId'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
            .body("'result'.'tools'[0].'inputSchema'.'required'.size()", equalTo(1))
            .body("'result'.'tools'[0].'inputSchema'.'required'[0]", containsString("destinationId"))
            .body("'result'.'tools'[1].'name'", containsString("list_destinations"))
            .body("'result'.'tools'[1].'description'", containsString("List all available holiday destinations with a short description."))
            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))


        val params = mutableMapOf<String, Any>()
        params["name"] = "get_destination_info"
        val destination = mutableMapOf<String, String>()
        destination["destinationId"] = "paris"
        params["arguments"] = destination
        val callToolBody = body(id, "tools/call", params)

        given()
            .contentType("application/json")
            .accept("application/json, text/event-stream")
            .body(callToolBody)
            .post("${baseUrlOfSut}/mcp")
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

    private fun body(id: Int, method: String, params: MutableMap<String, Any>): MutableMap<String, Any> {
        val request: MutableMap<String, Any> = mutableMapOf()
        request["jsonrpc"] = "2.0"
        request["id"] = id
        request["method"] = method
        request["params"] = params
        return request
    }

}
