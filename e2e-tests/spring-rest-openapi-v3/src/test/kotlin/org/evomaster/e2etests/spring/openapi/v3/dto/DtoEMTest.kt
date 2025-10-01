package org.evomaster.e2etests.spring.openapi.v3.dto

import com.foo.rest.examples.spring.openapi.v3.dto.DtoController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DtoEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DtoController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlaky(
            "DtoEM",
            "org.foo.DtoEM",
            100,
            true
        ) { args: MutableList<String> ->

            args.add("--dtoForRequestPayload")
            args.add("true")

            // TODO: Remove when handling Kotlin as DTOs and add reflective tests on checking if DTOs are created ok
            setOption(args, "outputFormat", OutputFormat.JAVA_JUNIT_4.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/object", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/array", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/array-of-string", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/string", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/number", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/integer", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/integer-no-format", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/boolean", "OK")
        }
    }

    override fun compile(outputFolderName: String?) {
        super.compile(outputFolderName)
    }
}
