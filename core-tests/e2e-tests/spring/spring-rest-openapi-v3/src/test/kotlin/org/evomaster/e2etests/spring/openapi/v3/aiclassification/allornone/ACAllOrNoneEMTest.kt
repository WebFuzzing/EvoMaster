package org.evomaster.e2etests.spring.openapi.v3.aiclassification.allornone

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneController
import org.evomaster.core.EMConfig.AIResponseClassifierModel
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.spring.openapi.v3.aiclassification.AIClassificationEMTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class ACAllOrNoneEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACAllOrNoneController())
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
            "ACAllOrNoneEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/allornone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/allornone", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/allornone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/allornone", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            //AllOrNone on x!=null and z=true
            //AllOrNone on a=false and d=HELLO

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/allornone",
                    queryParams = mapOf("x" to "foo", "y" to "42", "z" to "true"))!!,
                ptr.fromVerbPath("GET", "/api/allornone",
                    queryParams = mapOf("y" to "42", "z" to "false"))!!,
                ptr.fromVerbPath("POST", "/api/allornone",
                    jsonBodyPayload = """
                        { "a": false, "d": "HELLO", "b": "foo", "c": 42, "e": ["x","y","z"]}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/allornone",
                    jsonBodyPayload = """
                        { "d": "BAR", "c": -642, "e": ["x","y","z"]}
                    """.trimIndent())!!,
            )
            val failed = listOf(
                ptr.fromVerbPath("GET", "/api/allornone",
                    queryParams = mapOf("x" to "foo", "y" to "42", "z" to "false"))!!,
                ptr.fromVerbPath("GET", "/api/allornone",
                    queryParams = mapOf("z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/allornone",
                    jsonBodyPayload = """
                        { "a": true, "d": "HELLO", "b": "foo", "c": 42, "e": ["x","y","z"]}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/allornone",
                    jsonBodyPayload = """
                        { "a": false, "d": "BAR", "b": "foo", "c": 42, "e": ["x","y","z"]}
                    """.trimIndent())!!,
                )

            verifyModel(injector, ok, failed)
        }
    }
}
