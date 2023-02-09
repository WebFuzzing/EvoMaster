package org.evomaster.e2etests.spring.openapi.v3.wiremock.harvestoptimisation

import com.alibaba.dcm.DnsCacheManipulator
import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation.HarvestOptimisationController
import com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse.WmHarvestResponseController
import org.evomaster.ci.utils.CIUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.AfterAll
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

            /**
             * If the host name is localhost or starts with 127, host replacement will
             * skip it from handling external service. To avoid that fake host name used.
             */
            DnsCacheManipulator.setDnsCache("mock.int", "127.0.0.2")
        }

        @AfterAll
        @JvmStatic
        fun shutdown() {
            DnsCacheManipulator.clearDnsCache()
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "HarvestOptimisationEM",
            "org.foo.HarvestOptimisationEM",
            1000,
//            !CIUtils.isRunningGA(),
            false,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.3")
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