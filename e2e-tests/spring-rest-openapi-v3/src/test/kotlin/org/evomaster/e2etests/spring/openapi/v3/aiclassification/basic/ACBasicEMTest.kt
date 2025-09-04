package org.evomaster.e2etests.spring.openapi.v3.aiclassification.basic

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import com.google.inject.Injector
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.spring.openapi.v3.aiclassification.AIClassificationEMTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class ACBasicEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACBasicController())
        }
    }

    @Test
    fun testRunDeterministic(){
        testRunEM(AIResponseClassifierModel.DETERMINISTIC)
    }

    @Disabled
    @Test
    fun testRunGaussian(){
        testRunEM(AIResponseClassifierModel.GAUSSIAN)
    }

    private fun testRunEM(model: AIResponseClassifierModel) {

        runTestHandlingFlakyAndCompilation(
            "ACBasicEM",
            200
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/basic", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/basic", null)


            val ptr = injector.getInstance(PirToRest::class.java)

            verifyModel(injector,
                listOf(ptr.fromVerbPath("GET", "/api/basic", mapOf("y" to "42"))!!),
                listOf(ptr.fromVerbPath("GET", "/api/basic", mapOf("x" to "foo"))!!)
            )
        }
    }

}
