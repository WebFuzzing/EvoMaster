package org.evomaster.e2etests.spring.openapi.v3.gson

import com.foo.rest.examples.spring.openapi.v3.gson.GsonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class GsonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(GsonController())
        }
    }


    /**
     * Man: due to dynamic changes fields,
     * disable archive-based solution for the moment
     */
    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GsonEM",
                "org.foo.GsonEM",
                1000
        ) { args: MutableList<String> ->
            args.add("--weightBasedMutationRate")
            args.add("false")

            args.add("--probOfArchiveMutation")
            args.add("0.0")

            args.add("--adaptiveGeneSelectionMethod")
            args.add("NONE")

            args.add("--archiveGeneMutation")
            args.add("NONE")

            args.add("--enableTrackEvaluatedIndividual")
            args.add("true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/gson", "Hello World!!!")
        }
    }
}