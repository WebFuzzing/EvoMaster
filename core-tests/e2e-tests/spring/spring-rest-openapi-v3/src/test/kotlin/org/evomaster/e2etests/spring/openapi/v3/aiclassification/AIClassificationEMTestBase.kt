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
        repairThreshold: Double = injector.getInstance(EMConfig::class.java).classificationRepairThreshold,
        randomPerformanceThreshold: Double = 0.50
    ) {

        val model = injector.getInstance(AIResponseClassifier::class.java)
        model.disableLearning() // no side-effects

        var correctPrediction = 0
        // 400
        for (fail in fail400) {
            val result = evaluateAction(injector, fail)
            assertEquals(400, result.getStatusCode())

            val probability = model.classify(fail).probabilityOf400()
            if (probability >= repairThreshold) {
                correctPrediction++
            }
        }
        // 2xx
        for (ok in ok2xx) {
            val result = evaluateAction(injector, ok)
            assertTrue(result.getStatusCode() in 200..299)

            val probability = model.classify(ok).probabilityOf400()
            if (probability < repairThreshold) {
                correctPrediction++
            }
        }

        val totalSize = ok2xx.size + fail400.size
        val accuracy =
            if (totalSize > 0) correctPrediction.toDouble() / totalSize else 0.0

        assertTrue(
            accuracy > randomPerformanceThreshold,
            "Too low total accuracy: $accuracy"
        )
    }
}