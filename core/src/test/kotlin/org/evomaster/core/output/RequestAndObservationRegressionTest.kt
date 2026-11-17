package org.evomaster.core.output

import com.sun.net.httpserver.HttpServer
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.problem.httpws.HttpWsCallResult.ResponseField
import org.evomaster.core.problem.rest.data.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.net.InetSocketAddress
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType

class RequestAndObservationRegressionTest : WriterTestBase() {
    @TempDir lateinit var directory: Path

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun acceptIsTheRequestedTypeEvenForDifferentOrMissingResponseType(format: OutputFormat) {
        val received = LinkedBlockingQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") {
            received.add(it.requestHeaders.getFirst("Accept"))
            it.sendResponseHeaders(204, -1)
            it.close()
        }
        server.start()
        try {
            val writer = RestTestCaseWriter(getConfig(format), PartialOracles())
            val producesCases = listOf(emptyList(), listOf("application/json"), listOf("application/json", "text/plain"))
            for ((index, produces) in producesCases.withIndex()) {
                val action = RestCallAction("1", HttpVerb.GET, RestPath("/"), mutableListOf(), produces = produces)
                val snippets = listOf(null, MediaType.TEXT_PLAIN_TYPE, MediaType.APPLICATION_JSON_TYPE).map { type ->
                    val result = RestCallResult("1").apply { if (type != null) setBodyType(type) }
                    writer.getAcceptHeader(action, result)
                }
                assertEquals(1, snippets.distinct().size, "The response must not change the request")
                val statement = "io.restassured.RestAssured.given()${snippets.first()}.get(\"http://127.0.0.1:${server.address.port}/\")"
                val code = if (format.isJava()) "public class AcceptProbe { public void run() { $statement; } }"
                    else "class AcceptProbe { fun run() { $statement } }"
                GeneratedRegressionCompiler.withClass(directory.resolve("accept-$index"), format, "AcceptProbe", code) {
                    it.getMethod("run").invoke(it.getDeclaredConstructor().newInstance())
                }
                assertEquals(produces.firstOrNull() ?: "*/*", received.poll(5, TimeUnit.SECONDS))
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun missingValuesAreStoredAndSurviveCopyAndLaterMatchingRounds() {
        val original = RestCallResult("1").apply {
            setStatusCode(401); setBody("body"); setBodyType(MediaType.TEXT_PLAIN_TYPE)
            setErrorMessage("error")
            setHeaders(mapOf("WWW-Authenticate" to listOf("Basic"), "Upgrade" to listOf("TLS/1.2")))
        }
        val stable = original.copy()
        original.recordFlakyObservation(RestCallResult("1"), 1)
        original.recordFlakyObservation(stable, 2)
        val copy = original.copy()
        for (field in ResponseField.values()) {
            assertTrue(copy.hasFlakyField(field), field.name)
            assertEquals(listOf<String?>(null), copy.getFlakyValues(field), field.name)
            assertEquals(mapOf<Int, String?>(1 to null), copy.getFlakyVariation(field)!!.valuesByExecIndex)
        }
        assertTrue(stable.getFlakyValues(ResponseField.BODY).isEmpty())
    }

    private fun emit(format: OutputFormat, original: RestCallResult, observations: List<RestCallResult>): String {
        val action = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())
        val (_, baseUrl, individual) = EvaluatedIndividualBuilder.buildResourceEvaluatedIndividual(
            dbInitialization = mutableListOf(),
            groups = mutableListOf(mutableListOf<SqlAction>() to mutableListOf(action)), format = format)
        val result = individual.seeResult(action.getLocalId()) as RestCallResult
        result.setTimedout(false)
        result.setStatusCode(original.getStatusCode()!!)
        original.getBody()?.let { result.setBody(it) }
        original.getBodyType()?.let { result.setBodyType(it) }
        for (header in listOf("www-authenticate", "upgrade")) {
            original.getHeader(header)?.let { result.setHeaders(mapOf(header to listOf(it))) }
        }
        observations.forEachIndexed { i, observed -> result.recordFlakyObservation(observed, i + 1) }
        return RestTestCaseWriter(getConfig(format).apply { handleFlakiness = true }, PartialOracles())
            .convertToCompilableTestCode(TestCase(individual, "test"), baseUrl).toString()
    }

    private fun active(text: String) = text.lineSequence().map { it.substringBefore("//").trim() }.joinToString("\n")

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun completeGeneratedTestCompilesAndRunsWithMissingResponseFields(format: OutputFormat) {
        val original = RestCallResult("1").apply {
            setStatusCode(401); setBodyType(MediaType.APPLICATION_JSON_TYPE); setBody("{\"items\":[1]}")
            setHeaders(mapOf("WWW-Authenticate" to listOf("Basic realm=original")))
        }
        val missing = RestCallResult("1").apply { setStatusCode(401) }
        val responses = LinkedBlockingQueue<RestCallResult>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val response = responses.poll(5, TimeUnit.SECONDS)!!
            response.getHeader("www-authenticate")?.let { exchange.responseHeaders.set("WWW-Authenticate", it) }
            response.getBodyType()?.let { exchange.responseHeaders.set("Content-Type", it.toString()) }
            val body = response.getBody()?.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(response.getStatusCode()!!, body?.size?.toLong() ?: -1)
            if (body != null) exchange.responseBody.write(body)
            exchange.close()
        }
        server.start()
        try {
            val method = emit(format, original, listOf(missing, original))
            val url = "http://127.0.0.1:${server.address.port}"
            val code = if (format.isJava()) """
                import org.junit.jupiter.api.Test;
                import static io.restassured.RestAssured.given;
                import static org.hamcrest.Matchers.*;
                import static org.evomaster.client.java.controller.contentMatchers.NumberMatcher.numberMatches;
                public class ResponseProbe {
                    private String baseUrlOfSut = "$url";
                    $method
                }
            """.trimIndent() else """
                import org.junit.jupiter.api.Test
                import io.restassured.RestAssured.given
                import org.hamcrest.Matchers.*
                import org.evomaster.client.java.controller.contentMatchers.NumberMatcher.numberMatches
                class ResponseProbe {
                    private val baseUrlOfSut = "$url"
                    $method
                }
            """.trimIndent()
            GeneratedRegressionCompiler.withClass(directory.resolve("response"), format, "ResponseProbe", code) { klass ->
                val instance = klass.getDeclaredConstructor().newInstance()
                for (response in listOf(original, missing, original)) {
                    responses.add(response)
                    klass.getMethod("test").invoke(instance)
                }
                // The unchanged status assertion must still detect a new regression.
                responses.add(RestCallResult("1").apply { setStatusCode(500) })
                val failure = assertThrows(InvocationTargetException::class.java) { klass.getMethod("test").invoke(instance) }
                assertTrue(failure.cause is AssertionError, failure.toString())
            }
        } finally {
            server.stop(0)
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun headerChangesAndAbsenceAreFilteredButStableHeadersRemain(format: OutputFormat) {
        for ((status, header) in listOf(401 to "www-authenticate", 426 to "upgrade")) {
            for ((before, after) in listOf("first" to "second", "first" to null, null to "second")) {
                fun result(value: String?) = RestCallResult("1").apply {
                    setStatusCode(status)
                    if (value != null) setHeaders(mapOf(header.uppercase() to listOf(value)))
                }
                val original = result(before)
                val stable = emit(format, original, listOf(result(before)))
                assertTrue(active(stable).contains(".header(\"$header\""), stable)
                val changed = emit(format, original, listOf(result(after), result(before)))
                assertFalse(active(changed).contains(".header(\"$header\""), changed)
                assertTrue(changed.contains("// .header(\"$header\""), changed)
                assertTrue(active(changed).contains(".statusCode($status)"), changed)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun missingBodyAndTypeDoNotLeaveActiveAssertions(format: OutputFormat) {
        for (body in listOf("{\"items\":[1]}", "[1]", "42", "true", "null", "\"text\"", "plain text")) {
            val original = RestCallResult("1").apply {
                setStatusCode(200); setBody(body)
                setBodyType(if (body == "plain text") MediaType.TEXT_PLAIN_TYPE else MediaType.APPLICATION_JSON_TYPE)
            }
            val observed = RestCallResult("1").apply { setStatusCode(200) }
            val stable = emit(format, original, listOf(original))
            assertTrue(active(stable).contains(".body("), stable)
            val changed = emit(format, original, listOf(observed, original))
            assertFalse(active(changed).contains(".body("), changed)
            assertFalse(active(changed).contains(".contentType("), changed)
            assertTrue(active(changed).contains(".statusCode(200)"), changed)
            val noResponse = emit(format, original, listOf(RestCallResult("1")))
            assertFalse(active(noResponse).contains(".statusCode("), noResponse)
        }
    }
}
