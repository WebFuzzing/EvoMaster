package org.evomaster.e2etests.spring.openapi.v3.http429short

import com.foo.rest.examples.spring.openapi.v3.http429short.Http429ShortController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.search.service.Statistics.Companion.EVALUATED_ACTIONS
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class Http429ShortEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(Http429ShortController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlaky(
            "Http429ShortEM",
            "org.foo.Http429ShortEM",
            -1,
            true,
            { args: List<String> ->

                setOption(args, "stoppingCriterion", "TIME")
                setOption(args, "maxTime", "5s")
                setOption(args, "extraPhaseBudgetPercentage", "0")
                setOption(args, "minimize", "false")
                setOption(args, "security", "false")

                val (injector, solution) = initAndDebug(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/http429short", "OK")

                val statistics = injector.getInstance(Statistics::class.java)
                val data = statistics.getData(solution)
                val actions = data.first { it.header == EVALUATED_ACTIONS }.element.toInt()
                assertTrue(actions < 20, "Too many calls done in 5s when 429 with 1s: $actions")
            },
            3
        )
    }
}