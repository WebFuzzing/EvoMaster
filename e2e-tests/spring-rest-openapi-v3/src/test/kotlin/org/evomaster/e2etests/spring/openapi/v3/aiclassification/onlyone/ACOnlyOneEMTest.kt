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

   // @Disabled
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

            //TODO need to deal with body payload in PTR

            //verifyModel(injector) //TODO
        }
    }
}