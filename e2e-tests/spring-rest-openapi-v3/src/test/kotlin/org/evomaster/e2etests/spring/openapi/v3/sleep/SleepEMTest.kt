package org.evomaster.e2etests.spring.openapi.v3.sleep

import com.foo.rest.examples.spring.openapi.v3.sleep.SleepController
import other.bar.sleep.SleepCounter
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
/**
 *
 */

class SleepEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SleepController())
        }
    }


    @Test
    fun testRunEM() {

        
        val iterations = 5

        runTestHandlingFlakyAndCompilation(
            "SleepEM",
            "org.foo.SleepEM",
            iterations
        ) { args: MutableList<String> ->

            SleepCounter.counter.set(0)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            //minimization re-execute the test
            assertEquals(iterations+1, SleepCounter.counter.get())
        }
    }

}