package org.evomaster.e2etests.spring.openapi.v3.json.jackson.convert

import com.foo.rest.examples.spring.openapi.v3.json.jackson.convert.JacksonConvertValueController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled

class JacksonConvertValueEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(JacksonConvertValueController(), config)
        }
    }

    @Disabled
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JacksonConvertValueEM",
            "org.foo.JacksonConvertValueEM",
            500,
            true,
            { args: MutableList<String> ->
                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/jackson/convert", "Bingo!")
            },
            3
        )
    }
}
