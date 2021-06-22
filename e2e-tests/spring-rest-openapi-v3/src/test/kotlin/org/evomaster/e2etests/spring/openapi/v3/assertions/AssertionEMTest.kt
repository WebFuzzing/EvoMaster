package org.evomaster.e2etests.spring.openapi.v3.assertions

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

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

        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + ".kt")).noneMatch { l: String -> l.contains(".assertThat()") }

        assertTrue(assertion)
    }

    @Test
    fun testRunEMAssertionsOn() {
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionEMOn")

        testRunEMGeneric(true, className)

        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + ".kt")).anyMatch { l: String -> l.contains(".assertThat()") }

        assertTrue(assertion)
    }

    @Test
    fun testRunJavaAssertionsOn(){
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionJavaEMOn")

        testRunEMGeneric(true, className, "JAVA_JUNIT_5")

        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + ".java")).anyMatch { l: String -> l.contains(".assertThat()") }

        assertTrue(assertion)
    }

    @Test
    fun testRunJavaAssertionsOff(){
        val outputFolderName = "AssertionEM"
        val className = ClassName("org.foo.AssertionJavaEMOff")

        testRunEMGeneric(false, className, "JAVA_JUNIT_5")

        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className.getBytecodeName()
                + ".java")).noneMatch { l: String -> l.contains(".assertThat()") }

        assertTrue(assertion)
    }

    fun testRunEMGeneric(basicAssertions: Boolean, className: ClassName, outputFormat: String? = null){
        val outputFolderName = "AssertionEM"
        val iterations = 10_000

        val lambda = { args : MutableList<String> ->
            args.add("--enableBasicAssertions")
            args.add(basicAssertions.toString())

            if (outputFormat != null) {
                args.replaceAll { s -> s.replace("KOTLIN_JUNIT_5", outputFormat) }
            }

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

                lambda.invoke(args)
            }

        }

    }


}