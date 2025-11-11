package org.evomaster.e2etests.spring.openapi.v3.statistics

import com.foo.rest.examples.spring.openapi.v3.statistics.StatisticsController
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.search.service.Statistics
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class StatisticsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StatisticsController())
        }
    }

    @Test
    fun testRunEM(){
        val terminations = Arrays.asList(Termination.FAULTS.suffix,
                Termination.SUCCESSES.suffix)

        runTestHandlingFlakyAndCompilation(
                "StatisticsEM",
                "org.foo.StatisticsEM",
                terminations,
                1000
        ){args: MutableList<String> ->

            args.add("--addPreDefinedTests")
            args.add("false")

            val solution = initAndRun(args)

            val data = solution.statistics as MutableList<Statistics.Pair>

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)

            assertEquals("1", data.find { p -> p.header.contains("errors5xx") }?.element)
            assertEquals("1", data.find { p -> p.header.contains("distinct500Faults")}?.element)
            //assertEquals("1", data.find { p -> p.header.contains("failedOracleExpectations")}?.element)
            //assertEquals("3", data.find { p -> p.header.contains("potentialFaults")}?.element)


            listOf("coveredTargets", "coveredLines", "coveredBranches").forEach { key->
                data.filter { p-> p.header.endsWith(key, ignoreCase = true) }.apply {
                    assertEquals(4, size)
                    val bootTime = find { t-> t.header.startsWith("bootTime") }?.element?.toInt()
                    val searchTime = find { t-> t.header.startsWith("searchTime") }?.element?.toInt()
                    val seedingTime = find { t-> t.header.startsWith("seedingTime") }?.element?.toInt()
                    val total = find { t-> t.header.startsWith("covered") }?.element?.toInt()
                    assertNotNull(bootTime)
                    assertNotNull(searchTime)
                    assertNotNull(total)
                    assertNotNull(seedingTime)
                    /*
                        There are no branches in constructors...
                        but current problem is driver code in some package name...
                     */
                    //if(key != "coveredBranches") {
                        //there are no branches coverage at boot-time, as just default constructor
                        assertTrue(bootTime!! > 0)
                    //} else {
                      //  assertEquals(0, bootTime!!)
                    //}
                    assertTrue(searchTime!! > 0)
                    assertEquals(total!!, bootTime!!+searchTime!!+seedingTime!!)
                }
            }
        }
    }
}
