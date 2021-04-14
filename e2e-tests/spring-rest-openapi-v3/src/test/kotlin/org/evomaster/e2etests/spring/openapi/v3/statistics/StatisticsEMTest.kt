package org.evomaster.e2etests.spring.openapi.v3.statistics

import com.foo.rest.examples.spring.openapi.v3.statistics.StatisticsController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.search.service.Statistics
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StatisticsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            SpringTestBase.initClass(StatisticsController())
        }
    }

    @Test
    fun testRunEM(){
        runTestHandlingFlakyAndCompilation(
                "StatisticsEM",
                "org.foo.StatisticsEM",
                1000
        ){args: List<String> ->

            val solution = initAndRun(args)

            val data = solution.statistics as MutableList<Statistics.Pair>

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)

            assert(data.find { p -> p.header.contains("errors5xx") }?.element == "1")
            assert(data.find { p -> p.header.contains("distinct500Faults")}?.element == "1")
            assert(data.find { p -> p.header.contains("failedOracleExpectations")}?.element == "1")
            assert(data.find { p -> p.header.contains("potentialFaults")}?.element == "3")
        }
    }
}