package org.evomaster.e2etests.spring.openapi.v3.aiclassification.arithmetic

import com.foo.rest.examples.spring.openapi.v3.aiclassification.arithmetic.ACArithmeticController
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.spring.openapi.v3.aiclassification.AIClassificationEMTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class ACArithmeticEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACArithmeticController())
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

    private fun testRunEM(model: AIResponseClassifierModel) {

        runTestHandlingFlakyAndCompilation(
            "ACArithmeticEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/arithmetic", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/arithmetic", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/arithmetic", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/arithmetic", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            // query
            // x >= y
            // if z&&k then z>=k
            // body
            // x!=y && z>=k

            val ok = listOf(
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "-42", "y" to "4")
                )!!,

                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "-42", "y" to "4", "z" to "5", "k" to "5")
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"c":7, "e":6, "f":5, "g":-2}
                    """.trimIndent()
                )!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "42", "y" to "-4")
                )!!,
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "42", "y" to "-4", "z" to "-5", "k" to "45")
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"c":6, "e":6, "f":5, "g":-2}    
                    """.trimIndent()
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"c":7, "e":6, "f":5, "g":23}    
                    """.trimIndent()
                )!!
            )


            verifyModel(injector,ok,fail)
        }
    }
}
