package org.evomaster.e2etests.spring.openapi.v3.base

import com.foo.rest.examples.spring.openapi.v3.base.BaseController
import org.evomaster.core.output.OutputFormat
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class BaseMaxTimeAttemptEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseController())
        }
    }


    @Timeout(60)
    @Test
    fun testRunEM() {

            val args = listOf(
                    "--createTests", "false",
                    "--seed", "" + defaultSeed,
                    "--useTimeInFeedbackSampling", "false",
                    "--sutControllerPort", "" + controllerPort,
                    "--stoppingCriterion", "TIME",
                    "--createConfigPathIfMissing", "false",
                    "--maxTime", "10m", // way more than the JUnit @Timeout(60)
                    "--prematureStop", "5s" // short compared to JUnit @Timeout(60)
            )

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

    }
}