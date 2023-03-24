package org.evomaster.e2etests.spring.openapi.v3.wiremock.harvestresponse

import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.WmHarvestResponseController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WmHarvestResponseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(WmHarvestResponseController(), config)

            CIUtils.skipIfOnGA()
        }
    }


    @Test
    fun testRunEM() {
        // External service uses port 80 and 443 so the test will fail in macOS.
        runTestHandlingFlakyAndCompilation(
            "WmHarvestResponseEM",
            "org.foo.WmHarvestResponseEM",
            1000,
            !CIUtils.isRunningGA(),
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.22")
                args.add("--probOfHarvestingResponsesFromActualExternalServices")
                args.add("0.9")
                args.add("--probOfMutatingResponsesBasedOnActualResponse")
                args.add("0.1")

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/images", "ANY FROM ONE TO NINE")
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/images", "MORE THAN 10")
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/images", "NONE")

                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/grch37Example", "Found harvested response")
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/grch37Example", "Cannot find harvested response")

//                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/users", ">10")
//                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/users", "<10")
//                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvestresponse/users", "which has foo user")
            },
            3
        )
    }

}