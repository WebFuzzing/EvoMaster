package org.evomaster.e2etests.spring.openapi.v3.json.jackson.convert

import com.foo.rest.examples.spring.openapi.v3.json.jackson.convert.JacksonConvertValueController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JacksonConvertValueEM",
            500
        ) { args: MutableList<String> ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/jackson/convert", "Bingo!")
        }
    }
}
