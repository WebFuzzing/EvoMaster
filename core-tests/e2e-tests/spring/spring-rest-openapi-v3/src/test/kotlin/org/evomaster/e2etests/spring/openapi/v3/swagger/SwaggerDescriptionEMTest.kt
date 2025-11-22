package org.evomaster.e2etests.spring.openapi.v3.swagger

import com.foo.rest.examples.spring.openapi.v3.swagger.SwaggerDescriptionController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SwaggerDescriptionEMTest : SpringTestBase() {

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

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            val descriptions = mutableMapOf<String, String>()
            solution.individuals.forEach { actions ->
                val evaluatedAction = actions.evaluatedMainActions()[0].action
                if (evaluatedAction is RestCallAction) {
                    evaluatedAction.parameters
                        .forEach {
                            descriptions[it.name] = it.description.toString()

                            if (it.name == "body") {
                                it.seeGenes().forEach { gene ->
                                    if (!gene.description.isNullOrEmpty()) {
                                        descriptions[gene.name] = gene.description.toString()
                                    }
                                    if (gene.name == "body") {
                                        gene.getAllGenesInIndividual()
                                            .forEach { g ->
                                                if (!g.description.isNullOrEmpty()) {
                                                    descriptions[g.name] = g.description.toString()
                                                }
                                            }
                                    }
                                }
                            }
                        }
                }
            }

            // For now only header parameter description is available
            assertEquals(6, descriptions.size)
            assertEquals("Custom header for testing", descriptions["X-Custom-Header"])
            assertEquals("Returns a greeting message.", descriptions["body"])
            assertEquals("Name to be greeted", descriptions["name"])
            assertEquals("Age of the person", descriptions["age"])

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/v1", "GET is working")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/v1", "POST is working")
        }
    }
}
