package org.evomaster.e2etests.spring.openapi.v3.jackson

import com.foo.rest.examples.spring.openapi.v3.jackson.JacksonController
import org.evomaster.client.java.utils.SimpleLogger
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JacksonEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JacksonController())
        }
    }

    @Test
    fun testTypeReadValue() {
        LoggingUtil.getInfoLogger().info("Test init");
        runTestHandlingFlakyAndCompilation(
            "JacksonTypeEM",
            "org.foo.JacksonTypeEM",
            5000
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/type", "Working")
        }
    }

    @Disabled
    fun testGenericReadValue() {
        runTestHandlingFlakyAndCompilation(
            "JacksonGenericEM",
            "org.foo.JacksonGenericEM",
            500
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/generic", "Working")
        }
    }
}