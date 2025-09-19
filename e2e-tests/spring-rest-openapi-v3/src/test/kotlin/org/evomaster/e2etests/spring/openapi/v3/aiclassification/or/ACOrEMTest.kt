package org.evomaster.e2etests.spring.openapi.v3.aiclassification.or

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
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


class ACOrEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACOrController())
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
            "ACOrEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/or", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/or", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/or", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/or", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            // x or z=true
            // a=true or b
            // e or f=false

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/or",
                    queryParams = mapOf("x" to "foo"))!!,
                ptr.fromVerbPath("GET", "/api/or",
                    queryParams = mapOf("z" to "true"))!!,
                ptr.fromVerbPath("GET", "/api/or",
                    queryParams = mapOf("x" to "foo", "z" to "false"))!!,
                ptr.fromVerbPath("GET", "/api/or",
                    queryParams = mapOf("x" to "foo", "z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/or",
                    jsonBodyPayload = """
                        {"a": true, "e": "hi"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/or",
                    jsonBodyPayload = """
                        {"b": "bar", "f": false}
                    """.trimIndent())!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/or",
                    queryParams = mapOf("y" to "42"))!!,
                ptr.fromVerbPath("POST", "/api/or",
                    jsonBodyPayload = """
                        {"a": true}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/or",
                    jsonBodyPayload = """
                        {"e": "hi"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/or",
                    jsonBodyPayload = """
                        {"b": "bar", "f": true}
                    """.trimIndent())!!,
            )


            verifyModel(injector,ok,fail)
        }
    }
}