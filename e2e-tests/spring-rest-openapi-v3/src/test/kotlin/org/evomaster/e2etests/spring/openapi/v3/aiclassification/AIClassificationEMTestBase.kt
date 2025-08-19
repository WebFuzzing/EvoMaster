package org.evomaster.e2etests.spring.openapi.v3.aiclassification

import com.google.inject.Injector
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase

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

}