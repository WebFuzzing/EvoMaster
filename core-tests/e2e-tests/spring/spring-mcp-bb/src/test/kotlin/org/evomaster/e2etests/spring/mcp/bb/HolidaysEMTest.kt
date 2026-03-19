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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.indexOf
import kotlin.text.substring

class HolidaysEMTest: EnterpriseTestBase() {

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

    private var sseStream: InputStream? = null
    private var sessionId: String? = null
    private val httpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun openSseSession() {
        val request = HttpRequest.newBuilder(URI.create("$baseUrlOfSut/sse"))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val endpointRef = AtomicReference<String>()
        val latch = java.util.concurrent.CountDownLatch(1)

        CompletableFuture.runAsync {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            sseStream = response.body()
            val reader = BufferedReader(InputStreamReader(response.body()))
            var nextIsEndpointData = false
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                // Spring SseBuilder writes "event:name" (no space); tolerate "event: name" too
                if (l.trimEnd() == "event:endpoint" || l.trimEnd() == "event: endpoint") {
                    nextIsEndpointData = true
                } else if (nextIsEndpointData && l.startsWith("data:")) {
                    endpointRef.set(l.removePrefix("data:").trim())
                    latch.countDown()
                    nextIsEndpointData = false
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        val endpointPath = endpointRef.get()
            ?: throw RuntimeException("Timed out waiting for MCP endpoint event from SSE")
        sessionId = endpointPath.substringAfterLast("sessionId=")
    }

    @AfterEach
    fun closeSseSession() {
        sseStream?.close()
        sseStream = null
        sessionId = null
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

        given().accept("*/*")
            .header("x-EMextraHeader123", "42")
            .contentType("application/json")
            .body(body)
            .post("${baseUrlOfSut}/mcp/message?sessionId=$sessionId")
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

//    @Test
//    @Throws(Exception::class)
//    fun test_2_list_tools_returns_server_available_tools() {
//        val id = 1
//
//        val initParams = mutableMapOf<String, Any>()
//        initParams["protocolVersion"] = "2024-11-05"
//        val clientInfo = mutableMapOf<String, String>()
//        clientInfo["name"] = "test-client"
//        clientInfo["version"] = "1.0"
//        initParams["clientInfo"] = clientInfo
//        initParams["capabilities"] = emptyMap<String, Any>()
//
//        val initializeBody = body(id, "initialize", initParams)
//
//        val uuid = UUID.randomUUID().toString()
//
//        given().accept("*/*")
//            .header("x-EMextraHeader123", "42")
//            .contentType("application/json")
//            .body(initializeBody)
//            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
//            .then()
//            .statusCode(200)
//            .assertThat()
//            .contentType("application/json")
//            .body("'jsonrpc'", containsString("2.0"))
//            .body("'id'", `is`(id))
//            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
//            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
//            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))
//
//
//        val listToolsBody = body(id, "tools/list", mutableMapOf())
//        given().accept("*/*")
//            .header("x-EMextraHeader123", "42")
//            .contentType("application/json")
//            .body(listToolsBody)
//            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
//            .then()
//            .statusCode(200)
//            .assertThat()
//            .contentType("application/json")
//            .body("'jsonrpc'", containsString("2.0"))
//            .body("'id'", `is`(id))
//            .body("'result'.'tools'.size()", equalTo(2))
//            .body("'result'.'tools'[0].'name'", containsString("list_destinations"))
//            .body("'result'.'tools'[0].'description'", containsString("List all available holiday destinations with a short description."))
//            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
//            .body("'result'.'tools'[1].'name'", containsString("get_destination_info"))
//            .body("'result'.'tools'[1].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
//            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))
//            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'type'", containsString("string"))
//            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
//            .body("'result'.'tools'[1].'inputSchema'.'required'.size()", equalTo(1))
//            .body("'result'.'tools'[1].'inputSchema'.'required'[0]", containsString("destination"))
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun test_3_call_tool_with_destination() {
//        val id = 1
//
//        val initParams = mutableMapOf<String, Any>()
//        initParams["protocolVersion"] = "2024-11-05"
//        val clientInfo = mutableMapOf<String, String>()
//        clientInfo["name"] = "test-client"
//        clientInfo["version"] = "1.0"
//        initParams["clientInfo"] = clientInfo
//        initParams["capabilities"] = emptyMap<String, Any>()
//
//        val initializeBody = body(id, "initialize", initParams)
//
//        val uuid = UUID.randomUUID().toString()
//
//        given().accept("*/*")
//            .header("x-EMextraHeader123", "42")
//            .contentType("application/json")
//            .body(initializeBody)
//            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
//            .then()
//            .statusCode(200)
//            .assertThat()
//            .contentType("application/json")
//            .body("'jsonrpc'", containsString("2.0"))
//            .body("'id'", `is`(id))
//            .body("'result'.'protocolVersion'", containsString("2024-11-05"))
//            .body("'result'.'serverInfo'.'name'", containsString("holiday-mcp-server"))
//            .body("'result'.'serverInfo'.'version'", containsString("1.0.0"))
//
//
//        val listToolsBody = body(id, "tools/list", mutableMapOf())
//        given().accept("*/*")
//            .header("x-EMextraHeader123", "42")
//            .contentType("application/json")
//            .body(listToolsBody)
//            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
//            .then()
//            .statusCode(200)
//            .assertThat()
//            .contentType("application/json")
//            .body("'jsonrpc'", containsString("2.0"))
//            .body("'id'", `is`(id))
//            .body("'result'.'tools'.size()", equalTo(2))
//            .body("'result'.'tools'[0].'name'", containsString("list_destinations"))
//            .body("'result'.'tools'[0].'description'", containsString("List all available holiday destinations with a short description."))
//            .body("'result'.'tools'[0].'inputSchema'.'type'", containsString("object"))
//            .body("'result'.'tools'[1].'name'", containsString("get_destination_info"))
//            .body("'result'.'tools'[1].'description'", containsString("Get full details about a holiday destination: highlights, best travel months, weather, currency, and language."))
//            .body("'result'.'tools'[1].'inputSchema'.'type'", containsString("object"))
//            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'type'", containsString("string"))
//            .body("'result'.'tools'[1].'inputSchema'.'properties'.'destination'.'description'", containsString("Destination ID (e.g. bali, paris, tokyo). Use list_destinations to see all IDs."))
//            .body("'result'.'tools'[1].'inputSchema'.'required'.size()", equalTo(1))
//            .body("'result'.'tools'[1].'inputSchema'.'required'[0]", containsString("destination"))
//
//
//        val params = mutableMapOf<String, Any>()
//        params["name"] = "get_destination_info"
//        val destination = mutableMapOf<String, String>()
//        destination["destination"] = "paris"
//        params["arguments"] = destination
//        val callToolBody = body(id, "tools/call", params)
//
//        given().accept("*/*")
//            .header("x-EMextraHeader123", "42")
//            .contentType("application/json")
//            .body(callToolBody)
//            .post("${baseUrlOfSut}/messages?sessionId=$uuid")
//            .then()
//            .statusCode(200)
//            .assertThat()
//            .contentType("application/json")
//            .body("'jsonrpc'", containsString("2.0"))
//            .body("'id'", `is`(id))
//            .body("'result'.'content'.size()", equalTo(1))
//            .body("'result'.'content'[0].'type'", containsString("text"))
//            .body("'result'.'content'[0].'text'", containsString("The City of Light: world-class art, haute cuisine, grand boulevards, and the iconic Eiffel Tower."))
//    }

    private fun body(id: Int, method: String, params: MutableMap<String, Any>): MutableMap<String, Any> {
        val request: MutableMap<String, Any> = mutableMapOf()
        request["jsonrpc"] = "2.0"
        request["id"] = id
        request["method"] = method
        request["params"] = params
        return request
    }

}
