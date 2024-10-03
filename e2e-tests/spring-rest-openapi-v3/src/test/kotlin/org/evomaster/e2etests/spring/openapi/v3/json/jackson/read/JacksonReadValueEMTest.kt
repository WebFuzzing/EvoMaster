package org.evomaster.e2etests.spring.openapi.v3.json.jackson.read

import com.foo.rest.examples.spring.openapi.v3.json.jackson.read.JacksonReadValueController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JacksonReadValueEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(JacksonReadValueController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JacksonReadValueEM",
            "org.foo.JacksonReadValueEM",
            500,
            true,
            { args: MutableList<String> ->
                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/read", "OK")
            },
            3
        )
    }
}
