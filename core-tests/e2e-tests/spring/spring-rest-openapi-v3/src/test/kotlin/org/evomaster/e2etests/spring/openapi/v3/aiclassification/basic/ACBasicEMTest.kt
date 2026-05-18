package org.evomaster.e2etests.spring.openapi.v3.aiclassification.basic

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.spring.openapi.v3.aiclassification.AIClassificationEMTestBase
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

    @Disabled
    @Test
    fun testRunDeterministic(){
        testRunEM(AIResponseClassifierModel.DETERMINISTIC)
    }

    @Disabled
    @Test
    fun testRunGaussian(){
        testRunEM(AIResponseClassifierModel.GAUSSIAN)
    }

    @Disabled
    @Test
    fun testRunGLM(){
        testRunEM(AIResponseClassifierModel.GLM)
    }

    @Disabled
    @Test
    fun testRunKDE(){
        testRunEM(AIResponseClassifierModel.KDE)
    }

    @Disabled
    @Test
    fun testRunKNN(){
        testRunEM(AIResponseClassifierModel.KNN)
    }

    @Disabled
    @Test
    fun testRunNN(){
        testRunEM(AIResponseClassifierModel.NN)
    }

    @Test
    fun testRunEnsemble(){
        testRunEM(
            AIResponseClassifierModel.GAUSSIAN,
            AIResponseClassifierModel.GLM,
            AIResponseClassifierModel.KDE,
            AIResponseClassifierModel.KNN,
            AIResponseClassifierModel.NN
        )
    }

    private fun testRunEM(vararg models: AIResponseClassifierModel) {

        val modelString = models.joinToString(",") { it.name }

        runTestHandlingFlakyAndCompilation(
            "ACBasicEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add(modelString)

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
