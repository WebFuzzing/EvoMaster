package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.WriterTestBase
import org.evomaster.core.output.compiler.GeneratedRegressionCompiler
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path

/** Execute generated JVM assertions against all observations and a stable-field negative control. */
class ApiTestCaseWriterExecutionTest : WriterTestBase() {

    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun generatedAssertionsCompileAndExecuteAgainstEveryObservation(format: OutputFormat) {
        val original = """{"items":[1,2],"names":["A","B"],"nullable":1,"label":"abc","stable":7}"""
        val observations = listOf(
            """{"items":0,"names":["B","A"],"nullable":null,"label":"abcd","stable":7}""",
            """{"items":[3,2],"names":["A","B","C"],"nullable":1,"label":"abc","stable":7}"""
        )
        val result = RestCallResult("action", false).apply { setBody(original) }
        observations.forEachIndexed { index, body ->
            result.recordFlakyObservation(RestCallResult("action", false).apply { setBody(body) }, index + 1)
        }
        val lines = Lines(format)
        RestTestCaseWriter(getConfig(format), PartialOracles()).handleJsonStringAssertions(
            original, result.getFlakyBodies().orEmpty(), lines, null, false)
        val className = "GeneratedCumulativeAssertions"
        val code = if (format.isJava()) """
            import io.restassured.builder.ResponseBuilder;
            import static org.hamcrest.Matchers.*;
            import static org.evomaster.client.java.controller.contentMatchers.NumberMatcher.numberMatches;
            public class $className {
                public void verify(String body) {
                    new ResponseBuilder().setContentType("application/json").setBody(body).build().then()
                    $lines
                    ;
                }
            }
        """.trimIndent() else """
            import io.restassured.builder.ResponseBuilder
            import org.hamcrest.Matchers.*
            import org.evomaster.client.java.controller.contentMatchers.NumberMatcher.numberMatches
            class $className {
                fun verify(body: String) {
                    ResponseBuilder().setContentType("application/json").setBody(body).build().then()
                    $lines
                }
            }
        """.trimIndent()
        GeneratedRegressionCompiler.withClass(directory, format, className, code) { klass ->
            val instance = klass.getDeclaredConstructor().newInstance()
            val verify = klass.getMethod("verify", String::class.java)
            for (body in listOf(original) + observations) verify.invoke(instance, body)
            // Prove that the generated checks have not simply all been disabled.
            val failure = assertThrows(InvocationTargetException::class.java) {
                verify.invoke(instance, original.replace("\"stable\":7", "\"stable\":8"))
            }
            assertTrue(failure.cause is AssertionError, failure.toString())
        }
    }
}
