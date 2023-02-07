package org.evomaster.e2etests.spring.openapi.v3.wiremock.harvestoptimisation

import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation.HarvestOptimisationController
import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.WmHarvestResponseController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HarvestOptimisationEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(HarvestOptimisationController(), config)

            CIUtils.skipIfOnGA()
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "WmHarvestResponseEM",
            "org.foo.WmHarvestResponseEM",
            1000,
            !CIUtils.isRunningGA(),
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.2")
                args.add("--probOfHarvestingResponsesFromActualExternalServices")
                args.add("0.9")
                args.add("--probOfMutatingResponsesBasedOnActualResponse")
                args.add("0.1")

                val solution = initAndRun(args)

                Assertions.assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wm/harvester/external", "Working")
            },
            3
        )
    }
}