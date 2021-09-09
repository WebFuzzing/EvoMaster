package org.evomaster.e2etests.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.expectations.ExpectationsTestController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

class ExpectationEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init(){
            initClass(ExpectationsTestController())
        }
    }

    @Test
    fun testRunEMExpectationsOff() {
        val outputFolderName = "ExpectationsEM"
        val className = ClassName("org.foo.ExpectationEMOff")

        testRunEMGeneric(false, className)

        val assertion = generatedCodeAssertion(outputFolderName, className, OutputFormat.KOTLIN_JUNIT_5, false)
        assertTrue(assertion)
        compileRunAndVerifyTests(outputFolderName, className)
    }

    @Test
    fun testRunEMExpectationsOn() {
        val outputFolderName = "ExpectationsEM"
        val className = ClassName("org.foo.ExpectationEMOn")

        testRunEMGeneric(true, className)

        val assertion = generatedCodeAssertion(outputFolderName, className, OutputFormat.KOTLIN_JUNIT_5, true)
        assertTrue(assertion)
        compileRunAndVerifyTests(outputFolderName, className)
    }

    @Test
    fun testRunEMJavaExpectationsOff() {
        val outputFolderName = "ExpectationsEM"
        val className = ClassName("org.foo.ExpectationEMJavaOff")

        testRunEMGeneric(false, className, OutputFormat.JAVA_JUNIT_5)

        val assertion = generatedCodeAssertion(outputFolderName, className, OutputFormat.JAVA_JUNIT_5, false)
        assertTrue(assertion)
    }

    @Test
    fun testRunEMJavaExpectationsOn() {
        val outputFolderName = "ExpectationsEM"
        val className = ClassName("org.foo.ExpectationEMJavaOn")

        testRunEMGeneric(true, className, OutputFormat.JAVA_JUNIT_5)

        val assertion = generatedCodeAssertion(outputFolderName, className, OutputFormat.JAVA_JUNIT_5, true)
        assertTrue(assertion)
    }

    fun generatedCodeAssertion(outputFolderName: String, className: ClassName, outputFormat: OutputFormat, expectationsActive: Boolean): Boolean {
        val extension = when {
            outputFormat.isJava() -> ".java"
            outputFormat.isKotlin() -> ".kt"
            else -> return false
        }
        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + extension))

        return if(expectationsActive) assertion.anyMatch { l: String -> l.contains("expectationHandler.expect(ems)") }
        else assertion.noneMatch { l: String -> l.contains("expectationHandler.expect(ems)") }
    }

    fun testRunEMGeneric(expectationActive: Boolean, className: ClassName, outputFormat: OutputFormat? = OutputFormat.KOTLIN_JUNIT_5){
        val outputFolderName = "ExpectationsEM"
        val iterations = 1000

        val lambda = {args: MutableList<String> ->
            setOutputFormat(args, outputFormat)

            if(!expectationActive){
                val ind = args.indexOf("--expectationsActive") + 1
                args.set(ind, "false")
            }

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/basicResponsesString/{s}", "Success!")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/basicResponsesNumeric/{s}", "42")
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/expectations/basicResponsesNumeric/{s}", "")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/basicInput/{s}", "42")
            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/expectations/basicInput/{s}", "")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseObj/{s}", "successes")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseObj/{s}", "{")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseUnsupObj/{s}", "validObject_")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseUnsupObj/{s}", "object_")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseMultipleObjs/{s}", "[")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/responseMultipleObjs/{s}", "[")

        }

        assertTimeoutPreemptively(Duration.ofMinutes(3)) {
            clearGeneratedFiles(outputFolderName, className)
            handleFlaky {
                val args : MutableList<String> = getArgsWithCompilation(iterations,
                outputFolderName,
                className,
                true)
                setOutputFormat(args, outputFormat)

                lambda.invoke(args)
            }
        }

    }
}