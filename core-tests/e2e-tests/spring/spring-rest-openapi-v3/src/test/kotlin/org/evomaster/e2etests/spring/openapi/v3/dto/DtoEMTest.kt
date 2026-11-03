package org.evomaster.e2etests.spring.openapi.v3.dto

import com.foo.rest.examples.spring.openapi.v3.dto.DtoController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DtoEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DtoController())
        }
    }

    @ParameterizedTest
    @EnumSource(names = ["KOTLIN_JUNIT_5", "JAVA_JUNIT_5"])
    fun testRunEM(outputFormat: OutputFormat) {
        runTestHandlingFlakyAndCompilation(
            "DtoEM_$outputFormat",
            //DTOs are written next to the suite, so each format needs its own package
            "org.foo.${outputFormat.name.lowercase()}.DtoEM",
            100,
        ) { args: MutableList<String> ->

            setOption(args,"outputFormat",outputFormat.name)
            setOption(args,"dtoForRequestPayload","true")

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

}
