package org.evomaster.e2etests.spring.openapi.v3.aiclassification.defaultexample

import com.foo.rest.examples.spring.openapi.v3.aiclassification.defaultexample.ACDefaultExampleController
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.spring.openapi.v3.aiclassification.AIClassificationEMTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class ACDefaultExampleEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACDefaultExampleController())
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
            "ACDefaultExampleEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add(modelString)

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/defaultexample", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/defaultexample", null)


            val ptr = injector.getInstance(PirToRest::class.java)

            //constrain is that y>x if they are defined
            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/defaultexample")!!,
                ptr.fromVerbPath("GET", "/api/defaultexample", mapOf("y" to "42"))!!,
                ptr.fromVerbPath("GET", "/api/defaultexample", mapOf("y" to "42", "x" to "3"))!!,
                ptr.fromVerbPath("GET", "/api/defaultexample", mapOf("x" to "3"))!!
            )
            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/defaultexample", mapOf("x" to "1", "y" to "0"))!!,
                ptr.fromVerbPath("GET", "/api/defaultexample", mapOf("x" to "-2", "y" to "-5"))!!
            )

            verifyModel(injector, ok, fail)
        }
    }

}
