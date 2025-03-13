package org.evomaster.e2etests.spring.openapi.v3.swagger

import com.foo.rest.examples.spring.openapi.v3.swagger.SwaggerDescriptionController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SwaggerDescriptionEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SwaggerDescriptionController())
        }
    }

    @Test
    fun test() {
        runTestHandlingFlakyAndCompilation(
            "PerfectSwaggerEM",
            500
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)

            val descriptions = mutableMapOf<String, String>()
            solution.individuals.forEach { v ->
                val a = v.evaluatedMainActions()[0].action
                if (a is RestCallAction) {
                    a.parameters.forEach {
                        if (it.name != "body") {
                            descriptions[it.name] = it.getDescription().toString()
                        }
                    }
                }
            }

            // For now only header parameter description is available
            assertEquals(3, descriptions.size)
            assertEquals("Custom header for testing", descriptions["X-Custom-Header"])  // Actual value: Custom header for testing

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/v1", "GET is working")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/v1", "POST is working")
        }
    }
}
