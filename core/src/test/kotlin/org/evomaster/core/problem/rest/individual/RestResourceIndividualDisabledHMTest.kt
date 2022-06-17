package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.search.service.mutator.StandardMutator


class RestResourceIndividualDisabledHMTest : RestIndividualTestBase(){

    private lateinit var sampler : ResourceSampler
    private lateinit var mutator : ResourceRestMutator
    private lateinit var ff : RestFitness


    override fun config(): Array<String> {
        return arrayOf(
            "--enableTrackEvaluatedIndividual=false",
            "--weightBasedMutationRate=false",
            "--probOfArchiveMutation=0.0"
        )
    }

    override fun initService(injector: Injector) {

        sampler = injector.getInstance(ResourceSampler::class.java)
        mutator = injector.getInstance(ResourceRestMutator::class.java)
        ff = injector.getInstance(RestFitness::class.java)

    }

    override fun getProblemModule(): Module = ResourceRestModule(false)

    override fun getSampler() = sampler

    override fun getMutator(): StandardMutator<RestIndividual> = mutator

    override fun getFitnessFunction(): AbstractRestFitness<RestIndividual> = ff


}