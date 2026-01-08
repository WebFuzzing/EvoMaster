package org.evomaster.e2etests.spring.openapi.v3.dto

import com.foo.rest.examples.spring.openapi.v3.dto.DtoController
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
        runTestHandlingFlakyAndCompilation(
            "DtoEM",
            "org.foo.DtoEM",
            100,
        ) { args: MutableList<String> ->

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
