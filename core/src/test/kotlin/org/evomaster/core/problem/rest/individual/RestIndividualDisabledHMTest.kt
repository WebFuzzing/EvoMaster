package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.fitness.RestFitness
import org.evomaster.core.problem.rest.service.module.RestModule
import org.evomaster.core.problem.rest.service.sampler.RestSampler
import org.evomaster.core.search.service.mutator.StandardMutator


/**
 * tests are to check sampler and mutator which do not employ
 * resource-based strategies and adaptive hypermutation
 */
class RestIndividualDisabledHMTest : RestIndividualTestBase(){

    private lateinit var sampler : RestSampler
    private lateinit var mutator : StandardMutator<RestIndividual>
    private lateinit var ff : RestFitness


    override fun config(): Array<String> {
        return arrayOf(
            "--enableTrackEvaluatedIndividual=false",
            "--weightBasedMutationRate=false",
            "--probOfArchiveMutation=0.0"
        )
    }

    override fun initService(injector: Injector) {

        sampler = injector.getInstance(RestSampler::class.java)
        mutator = injector.getInstance(Key.get(
            object : TypeLiteral<StandardMutator<RestIndividual>>() {}))
        ff = injector.getInstance(RestFitness::class.java)

    }

    override fun getProblemModule(): Module = RestModule(false)

    override fun getSampler() = sampler

    override fun getMutator(): StandardMutator<RestIndividual> = mutator

    override fun getFitnessFunction(): AbstractRestFitness = ff


}