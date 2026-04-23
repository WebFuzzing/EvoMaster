package org.evomaster.e2etests.spring.openapi.v3.base

import com.foo.rest.examples.spring.openapi.v3.base.BaseController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class BaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "BaseEM",
                "org.foo.BaseEM",
                20
        ) { args: MutableList<String> ->

            val executedMainActionToFile = "target/executionInfo/org/foo/BaseEM/executedMainActions.txt"

            args.add("--recordExecutedMainActionInfo")
            args.add("true")
            args.add("--saveExecutedMainActionInfo")
            args.add(executedMainActionToFile)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/base", "Hello World")

            val size = Files.readAllLines(Paths.get(executedMainActionToFile)).count { !it.contains("ComputationOverhead") && it.isNotBlank() }
            assertTrue(size in 20..21)
        }
    }
}