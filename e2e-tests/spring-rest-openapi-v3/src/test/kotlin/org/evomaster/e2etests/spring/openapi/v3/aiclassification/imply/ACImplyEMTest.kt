package org.evomaster.e2etests.spring.openapi.v3.aiclassification.imply

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneController
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


class ACImplyEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACImplyController())
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
            "ACImplyEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/imply", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/imply", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/imply", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/imply", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            //z==true implies x!=null
            //a==true implies d==HELLO or f==HELLO

            val ok = listOf(
                ptr.fromVerbPath("GET","/api/imply",
                    queryParams = mapOf("y" to "45"))!!,
                ptr.fromVerbPath("GET","/api/imply",
                    queryParams = mapOf("z" to "true", "x" to "foo"))!!,
                ptr.fromVerbPath("POST","/api/imply",
                    jsonBodyPayload = """
                        {"b": "bar"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST","/api/imply",
                    jsonBodyPayload = """
                        {"a": true, "d": "HELLO"}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST","/api/imply",
                    jsonBodyPayload = """
                        {"a": true, "d": "X", "f": "HELLO"}
                    """.trimIndent())!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET","/api/imply",
                    queryParams = mapOf("z" to "true", "y" to "45"))!!,
                ptr.fromVerbPath("POST","/api/imply",
                    jsonBodyPayload = """
                        {"a": true}
                    """.trimIndent())!!,
                ptr.fromVerbPath("POST","/api/imply",
                    jsonBodyPayload = """
                        {"a": true, "d": "X", "f": "X"}
                    """.trimIndent())!!,
            )

            verifyModel(injector,ok,fail)
        }
    }
}
