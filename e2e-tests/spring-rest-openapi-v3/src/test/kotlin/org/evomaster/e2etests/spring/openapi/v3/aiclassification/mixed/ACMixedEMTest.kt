package org.evomaster.e2etests.spring.openapi.v3.aiclassification.mixed

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.basic.ACBasicController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.imply.ACImplyController
import com.foo.rest.examples.spring.openapi.v3.aiclassification.mixed.ACMixedController
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


class ACMixedEMTest : AIClassificationEMTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ACMixedController())
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
            "ACMixedEM",
            500
        ) { args: MutableList<String> ->

            args.add("--aiModelForResponseClassification")
            args.add("$model")

            val (injector, solution) = initAndDebug(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/mixed", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/mixed", null)

            val ptr = injector.getInstance(PirToRest::class.java)

            // x<=y
            // ! (a&&b)
            // s!= null
            // if c then d=HELLO

            val ok = listOf(
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3", "s" to "hello there"))!!,
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3", "s" to "hello there", "a" to "true", "b" to "false", "c" to "false"))!!,
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3", "s" to "hello there", "c" to "true", "d" to "HELLO"))!!,
            )

            val fail = listOf(
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "42", "y" to "3", "s" to "hello there"))!!,
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3"))!!,
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3", "s" to "hello there", "a" to "true", "b" to "true", "c" to "false"))!!,
                ptr.fromVerbPath("GET", "/api/mixed",
                    queryParams = mapOf("x" to "0", "y" to "3", "s" to "hello there", "c" to "true", "d" to "Z"))!!,
            )

            verifyModel(injector, ok, fail)
        }
    }
}