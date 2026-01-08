package org.evomaster.e2etests.spring.openapi.v3.minimize

import com.foo.rest.examples.spring.openapi.v3.minimize.MinimizeController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class MinimizeEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MinimizeController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "MinimizeEM",
                "org.foo.MinimizeEM",
                5
        ) { args: MutableList<String> ->

            args.add("--minimize")
            args.add("true")
            args.add("--maxTestSize")
            args.add("1000")
            args.add("--probOfSmartSampling")
            args.add("0")
            args.add("--enableOptimizedTestSize")
            args.add("false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 5) //at least 5 distinct, mutually exclusive branches
            assertTrue(solution.individuals.all{it.individual.size() == 1})
        }
    }
}