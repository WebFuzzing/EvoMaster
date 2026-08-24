package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.verbselection.HttpInvalidLocationVerbSelectionController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpInvalidLocationVerbSelectionEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidLocationVerbSelectionController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpInvalidLocationVerbSelectionEM",
                50
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_INVALID_LOCATION }

            // Family A: Location -> declared GET returning 500. A fault here can only come from
            // the 500 status, proving 500 is part of the invalid-location status set.
            assertTrue(faults.any { it.operationId.contains("/api/a/") })
            // Family B: Location -> declared GET returning 501. Proves 501.
            assertTrue(faults.any { it.operationId.contains("/api/b/") })
            // Family C: Location -> PUT/PATCH-only target returning 405. Proves 405.
            assertTrue(faults.any { it.operationId.contains("/api/c/") })

            // Verb selection is proven by inspecting the generated follow-up calls (the last
            // action, chained to the previous Location via usePreviousLocationId).
            val followUps = solution.individuals.mapNotNull { ei ->
                val actions = ei.individual.seeMainExecutableActions()
                if (actions.size < 2) return@mapNotNull null
                val creator = actions[actions.size - 2]
                val follow = actions[actions.size - 1]
                if (follow.usePreviousLocationId.isNullOrBlank()) null else Pair(creator, follow)
            }

            // Family C target declares only PUT and PATCH; priority order must pick PUT (not PATCH,
            // and certainly not a default GET).
            assertTrue(followUps.any { (creator, follow) ->
                creator.path.toString().contains("/api/c/") && follow.verb == HttpVerb.PUT
            })
            // Family A target declares GET, so GET must be selected.
            assertTrue(followUps.any { (creator, follow) ->
                creator.path.toString().contains("/api/a/") && follow.verb == HttpVerb.GET
            })
        }
    }
}
