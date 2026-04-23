package org.evomaster.e2etests.spring.openapi.v3.assertions

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.stream.Stream

/**
 *
 */

class AssertionEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AssertionController())
        }
    }



    @Test
    fun testRunEMAssertionsOff() {
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionEMOff")

        testRunEMGeneric(false, className)

        val lines = getFileLines(outputFolderName, className, OutputFormat.KOTLIN_JUNIT_5)
        val assertion = generatedCodeAssertion(lines, false)

        assertTrue(assertion)

        compileRunAndVerifyTests(outputFolderName, className)

    }

    @Test
    fun testRunEMAssertionsOn() {
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionEMOn")
        val outputFormat = OutputFormat.KOTLIN_JUNIT_5

        testRunEMGeneric(true, className)
        compileRunAndVerifyTests(outputFolderName, className)

        val lines = getFileLines(outputFolderName, className, outputFormat)
        val assertion = generatedCodeAssertion(lines, true)

        assertTrue(assertion)

        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/data", "201", "text/plain", "OK")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleNumber", "200", "application/json", "42")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleString", "200", "application/json", "simple-string")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleText", "200", "text/plain", "simple-text")))

        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleArray", "200", "application/json", "123")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/arrayObject", "200", "application/json", "777")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/arrayEmpty", "200", "application/json", "[]")))

        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/objectEmpty", "200", "application/json", "{}")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/data", "200", "application/json", "a", "42", "c", "1000", "2000", "d", "66", "bar", "xvalue", "yvalue", "true", "false")))



    }

    @Test
    fun testRunJavaAssertionsOn(){
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionJavaEMOn")
        val outputFormat = OutputFormat.JAVA_JUNIT_5

        testRunEMGeneric(true, className, outputFormat)

        val assertion = generatedCodeAssertion(getFileLines(outputFolderName, className, outputFormat), true)
        assertTrue(assertion)


        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/data", "201", "text/plain", "OK")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleNumber", "200", "application/json", "42")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleString", "200", "application/json", "simple-string")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleText", "200", "text/plain", "simple-text")))

        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/simpleArray", "200", "application/json", "123")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/arrayObject", "200", "application/json", "777")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/arrayEmpty", "200", "application/json", "[]")))

        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/objectEmpty", "200", "application/json", "{}")))
        assertTrue(assertExistsInCode(outputFolderName, className, outputFormat,
                listOf("/api/assertions/data", "200", "application/json", "a", "42", "c", "1000", "2000", "d", "66", "bar", "xvalue", "yvalue", "true", "false")))

    }

    @Test
    fun testRunJavaAssertionsOff(){
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionJavaEMOff")

        testRunEMGeneric(false, className, OutputFormat.JAVA_JUNIT_5)
        val lines = getFileLines(outputFolderName, className, OutputFormat.JAVA_JUNIT_5)
        val assertion = generatedCodeAssertion(lines, false)

        assertTrue(assertion)
    }

    fun getFileLines(outputFolderName: String,
                     className: ClassName,
                     outputFormat: OutputFormat): Stream<String> {
        val extension = when {
            outputFormat.isJava() -> ".java"
            outputFormat.isKotlin() -> ".kt"
            else -> throw IllegalArgumentException("Only Java and Kotlin files are supported for now.")
        }
        val lines = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + extension))
        return lines
    }

    fun generatedCodeAssertion(lines: Stream<String>,
                               basicAssertions: Boolean): Boolean {

        return if(basicAssertions) lines.anyMatch { l: String -> l.contains(".assertThat()") }
            else lines.noneMatch { l: String -> l.contains(".assertThat()") }
    }

    /**
     * The method checks that all the Strings in [contents] are present in the file.
     */
    fun assertExistsInCode(outputFolderName: String, className: ClassName, outputFormat: OutputFormat, contents: List<String>): Boolean{
        val lines = getFileLines(outputFolderName, className, outputFormat)
        return contents.any { lines.anyMatch { line -> line.contains(it)} }
    }


    fun testRunEMGeneric(basicAssertions: Boolean, className: ClassName, outputFormat: OutputFormat? = OutputFormat.KOTLIN_JUNIT_5){
        val outputFolderName = "AssertionEM"
        val iterations = 100

        val lambda = { args : MutableList<String> ->
            args.add("--enableBasicAssertions")
            args.add(basicAssertions.toString())

            setOutputFormat(args, outputFormat)

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/assertions/data", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/data", "{\"a\":42,\"c\":[1000,2000,3000],\"d\":{\"e\":66,\"f\":\"bar\",\"g\":{\"h\":[\"xvalue\",\"yvalue\"]}},\"i\":true,\"l\":false}")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleNumber", "42")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleString", "simple-string")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleText", "simple-text")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleArray", "123")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/arrayObject", "777")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/arrayEmpty", "[]")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/objectEmpty", "{}")
        }

        assertTimeoutPreemptively(Duration.ofMinutes(3)) {
            clearGeneratedFiles(outputFolderName, className)

            handleFlaky {
                val args: MutableList<String> = getArgsWithCompilation(iterations,
                        outputFolderName,
                        className,
                        true)
                setOutputFormat(args, outputFormat)
                lambda.invoke(args)
            }
        }

    }


}