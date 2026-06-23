package org.evomaster.e2etests.spring.openapi.v3.http429long

import com.foo.rest.examples.spring.openapi.v3.http429long.Http429LongController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.search.service.Statistics.Companion.EVALUATED_ACTIONS
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class Http429LongEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(Http429LongController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlaky(
            "Http429LongEM",
            "org.foo.Http429LongEM",
            -1,
            true,
            { args: List<String> ->

                setOption(args, "stoppingCriterion", "TIME")
                setOption(args, "maxTime", "5s")
                setOption(args, "extraPhaseBudgetPercentage", "0")
                setOption(args, "minimize", "false")
                setOption(args, "security", "false")

                val (injector, solution) = initAndDebug(args)

                val statistics = injector.getInstance(Statistics::class.java)
                val data = statistics.getData(solution)
                val actions = data.first { it.header == EVALUATED_ACTIONS }.element.toInt()
                assertTrue(actions < 2, "Too many calls done in 5s when 429 with 1d: $actions")
            },
            3
        )
    }
}