package org.evomaster.e2etests.spring.openapi.v3.aiclassification.required

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
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


class ACRequiredEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACRequiredController())
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
            "ACRequiredEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/required", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/required", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/required", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/required", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT, 204, "/api/required", null) // OK not returned due to 204
            assertHasAtLeastOne(solution, HttpVerb.PUT, 400, "/api/required", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/required",
                    queryParams = mapOf("x" to "foo", "y" to "42", "z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/required",
                    jsonBodyPayload = """
                        {"b": "Bar"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("PUT", "/api/required",
                    jsonBodyPayload = """
                        {"a": false, "b": "Bar", "c": 42}
                    """.trimIndent(),
                    queryParams = mapOf("x" to "foo"))!!
            )

            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/required",
                    queryParams = mapOf("x" to "foo", "y" to "42"))!!,
                ptr.fromVerbPath("GET", "/api/required",
                    queryParams = mapOf("x" to "foo", "z" to "true"))!!,
                ptr.fromVerbPath("GET", "/api/required",
                    queryParams = mapOf("y" to "42", "z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/required",
                    jsonBodyPayload = """
                        {}
                    """.trimIndent())!!,
                ptr.fromVerbPath("PUT", "/api/required",
                    jsonBodyPayload = """
                        {"a": false, "b": "Bar"}
                    """.trimIndent(),
                    queryParams = mapOf("x" to "foo"))!!,
                ptr.fromVerbPath("PUT", "/api/required",
                    jsonBodyPayload = """
                        {"a": false, "b": "Bar", "c": 42}
                    """.trimIndent(),
                    queryParams = mapOf())!!            )

            verifyModel(injector,ok,fail)
        }
    }
}