package org.evomaster.e2etests.spring.openapi.v3.aiclassification

import com.google.inject.Injector
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

abstract class AIClassificationEMTestBase : SpringTestBase(){

    fun evaluateAction(injector: Injector, action: RestCallAction) : RestCallResult{

        val ind = createIndividual(injector, listOf(action))

        return ind.evaluatedMainActions().first().result as RestCallResult
    }

    fun createIndividual(
        injector: Injector,
        actions: List<RestCallAction>,
        sampleT : SampleType = SampleType.SEEDED
    ): EvaluatedIndividual<RestIndividual> {

        val sampler = injector.getInstance(AbstractRestSampler::class.java)

        val ind = sampler.createIndividual(sampleT, actions.toMutableList())

        val ff = injector.getInstance(AbstractRestFitness::class.java)
        val ei = ff.calculateCoverage(ind)!!

        return ei
    }

    protected fun verifyModel(
        injector: Injector,
        ok2xx: List<RestCallAction>,
        fail400: List<RestCallAction>,
        threshold: Double = injector.getInstance(EMConfig::class.java).classificationRepairThreshold,
        minimalAccuracy: Double = 0.5
    ) {

        val model = injector.getInstance(AIResponseClassifier::class.java)
        model.disableLearning() // no side-effects

        val accuracy = model.estimateOverallAccuracy()
        assertTrue(accuracy >= minimalAccuracy, "Too low accuracy $accuracy." +
                " Minimal accepted is $minimalAccuracy")

        for(ok in ok2xx){
            val resOK = evaluateAction(injector, ok)
            assertTrue(resOK.getStatusCode() in 200..299)
            val mOK= model.classify(ok)
            assertTrue(
                mOK.probabilityOf400() < threshold,
                "Too high probability of 400 for OK ${ok.getName()}: ${mOK.probabilityOf400()}")
        }

        for(fail in fail400) {
            val resFail = evaluateAction(injector, fail)
            assertEquals(400, resFail.getStatusCode())
            val mFail = model.classify(fail)
            assertTrue(
                mFail.probabilityOf400() >= threshold,
                "Too low probability of 400 for Fail ${fail.getName()}: ${mFail.probabilityOf400()}"
            )
        }
    }

}
