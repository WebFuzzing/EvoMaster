package org.evomaster.core.output

import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider

class CumulativeFlakyAssertionsExecutionTest : WriterTestBase() {

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
        val source = directory.resolve(className + if (format.isJava()) ".java" else ".kt")
        Files.writeString(source, code)
        if (format.isJava()) {
            val compiler = ToolProvider.getSystemJavaCompiler()
            assertNotNull(compiler, "A JDK is required to compile generated Java assertions")
            assertEquals(0, compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                "-d", directory.toString(), source.toString()), code)
        } else {
            CompilerForTestGenerated.compile(format, source.toFile(), directory.toFile())
        }
        URLClassLoader(arrayOf(directory.toUri().toURL()), javaClass.classLoader).use { loader ->
            val klass = loader.loadClass(className)
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
