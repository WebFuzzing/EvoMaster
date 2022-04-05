package org.evomaster.e2etests.spring.openapi.v3.taintkotlinequal

import com.foo.rest.examples.spring.openapi.v3.taintkotlinequal.TaintKotlinEqualController
import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.instrumentation.InstrumentingAgent
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TaintKotlinEqualEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            /*
                needed because kotlin.jvm.internal.Intrinsics gets loaded in
                TaintKotlinEqualController before agent is initialized
             */
            InstrumentedSutStarter.loadAgent()
            InstrumentingAgent.changePackagesToInstrument("com.foo.")
            initClass(TaintKotlinEqualController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "TaintKotlinEqualEM",
                "org.foo.TaintKotlinEqualEM",
                1_000
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintkotlinequal/{a}", "OK")
        }
    }
}