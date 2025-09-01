package org.evomaster.e2etests.spring.openapi.v3.aiclassification.basic

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import com.google.inject.Injector
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
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
            "org.foo.ACBasicEM",
            100
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/basic", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/basic", null)

            verifyModel(injector)
        }
    }

    private fun verifyModel(injector: Injector) {

        val model = injector.getInstance(AIResponseClassifier::class.java)
        model.disableLearning() // no side-effects

        val ptr = injector.getInstance(PirToRest::class.java)

        val ok = ptr.fromVerbPath("GET", "/api/basic", mapOf("y" to "42"))!!
        val resOK = evaluateAction(injector, ok)

        val fail = ptr.fromVerbPath("GET", "/api/basic", mapOf("x" to "foo"))!!
        val resFail = evaluateAction(injector, fail)

        assertEquals(200, resOK.getStatusCode())
        assertEquals(400, resFail.getStatusCode())

        //TODO later on, might want to have it in EMConfig
        val threshold = 0.9

        val mOK= model.classify(ok)
        assertTrue(mOK.probabilityOf400() < threshold, "Too high probability of 400 for OK: ${mOK.probabilityOf400()}")
        val mFail= model.classify(fail)
        assertTrue(mFail.probabilityOf400() >= threshold, "Too low probability of 400 for Fail: ${mFail.probabilityOf400()}")
    }
}