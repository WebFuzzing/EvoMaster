package org.evomaster.e2etests.spring.openapi.v3.aiclassification.arithmetic

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.arithmetic.ACArithmeticController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.imply.ACImplyController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone.ACOnlyOneController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.or.ACOrController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.required.ACRequiredController
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
import kotlin.math.max


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
                    queryParams = mapOf("x" to "42", "y" to "-4")
                )!!,
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "42", "y" to "-4", "z" to "5", "k" to "5")
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"x":7, "y":6, "z":5, "k":-2}    
                    """.trimIndent()
                )!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "-42", "y" to "+4")
                )!!,
                ptr.fromVerbPath("GET","/api/arithmetic",
                    queryParams = mapOf("x" to "42", "y" to "-4", "z" to "-5", "k" to "45")
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"x":6, "y":6, "z":5, "k":-2}    
                    """.trimIndent()
                )!!,
                ptr.fromVerbPath("POST","/api/arithmetic",
                    jsonBodyPayload = """
                        {"x":7, "y":6, "z":5, "k":23}    
                    """.trimIndent()
                )!!
            )

            verifyModel(injector,ok,fail)
        }
    }
}
