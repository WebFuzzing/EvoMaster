package org.evomaster.e2etests.spring.openapi.v3.examplepool

import com.foo.rest.examples.spring.openapi.v3.examplepool.ExamplePoolController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExamplePoolEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExamplePoolController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "ExamplePool",
                100
        ) { args: MutableList<String> ->

            setOption(args, "useObjectExampleDataPool", "true")
            setOption(args, "blackBox", "true")
            setOption(args, "base", baseUrlOfSut)
            setOption(args, "schema", "src/main/resources/static/openapi-examplepool.yaml")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/examplepool", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/examplepool", "OK")
        }
    }
}