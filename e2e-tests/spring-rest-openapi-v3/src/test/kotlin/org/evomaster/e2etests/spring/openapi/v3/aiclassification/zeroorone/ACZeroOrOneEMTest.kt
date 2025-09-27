package org.evomaster.e2etests.spring.openapi.v3.aiclassification.zeroorone

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.onlyone.ACOnlyOneController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.or.ACOrController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.required.ACRequiredController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.zeroorone.ACZeroOrOneController
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


class ACZeroOrOneEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACZeroOrOneController())
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
            "ACZeroOrOneEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/zeroorone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/zeroorone", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/zeroorone", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/zeroorone", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            // ZeroOrOne(x,z=true)
            // ZeroOrOne(d=HELLO,f=X)
            // ZeroOrOne(a!=true,b!=null)

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/zeroorone",
                    queryParams = mapOf("x" to "bar"))!!,
                ptr.fromVerbPath("GET", "/api/zeroorone",
                    queryParams = mapOf("z" to "true"))!!,
                ptr.fromVerbPath("GET", "/api/zeroorone",
                    queryParams = mapOf())!!,
                ptr.fromVerbPath("POST", "/api/zeroorone",
                    jsonBodyPayload = """
                        {}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/zeroorone",
                    jsonBodyPayload = """
                        {"d": "HELLO"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/zeroorone",
                    jsonBodyPayload = """
                        {"a": false}
                    """.trimIndent())!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/zeroorone",
                    queryParams = mapOf("x" to "bar", "z" to "true"))!!,
                ptr.fromVerbPath("POST", "/api/zeroorone",
                    jsonBodyPayload = """
                        {"d": "HELLO", "f": "X"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST", "/api/zeroorone",
                    jsonBodyPayload = """
                        {"a": false, "b": "bar"}
                    """.trimIndent())!!,
            )

            verifyModel(injector,ok,fail)
        }
    }
}