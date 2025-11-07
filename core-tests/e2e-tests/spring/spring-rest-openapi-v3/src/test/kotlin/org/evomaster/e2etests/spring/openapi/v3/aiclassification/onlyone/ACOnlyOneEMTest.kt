package org.evomaster.e2etests.spring.openapi.v3.aiclassification.onlyone

import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
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


class ACOnlyOneEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACOnlyOneController())
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
            "ACOnlyOneEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/onlyone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/onlyone", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/onlyone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/onlyone", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            // OnlyOne(x,z=true)
            // OnlyOne(a=false,d=FOO)

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/onlyone",
                    queryParams = mapOf("x" to "foo"))!!,
                ptr.fromVerbPath("GET", "/api/onlyone",
                    queryParams = mapOf("z" to "true"))!!,
                ptr.fromVerbPath("GET", "/api/onlyone",
                    queryParams = mapOf("x" to "foo", "z" to "false"))!!,
                ptr.fromVerbPath("POST", "/api/onlyone",
                    jsonBodyPayload = """
                        {"a": false, "b": "bar", "c": -3, "d": "BAR", "e": null}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/onlyone",
                    jsonBodyPayload = """
                        {"a": true, "b": "bar", "c": -3, "d": "FOO", "e": null}
                    """.trimIndent())!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/onlyone",
                    queryParams = mapOf())!!,
                ptr.fromVerbPath("GET", "/api/onlyone",
                    queryParams = mapOf("x" to "foo", "z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/onlyone",
                    jsonBodyPayload = """
                        {"a": false, "b": "bar", "c": -3, "d": "FOO", "e": null}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/onlyone",
                    jsonBodyPayload = """
                        {"b": "bar", "c": -3, "e": null}
                    """.trimIndent())!!,
                )

            verifyModel(injector,ok,fail)
        }
    }
}