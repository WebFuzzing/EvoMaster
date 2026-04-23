package org.evomaster.core.problem.rest.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.rest.service.fitness.BlackBoxRestFitness
import org.evomaster.core.problem.rest.service.sampler.RestSampler
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.rest.service.mutator.RestStructureMutator
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

class BlackBoxRestModule(
        val usingRemoteController: Boolean
): RestBaseModule(){

    override fun configure() {

        super.configure()

        bind(object : TypeLiteral<EnterpriseSampler<RestIndividual>>() {})
            .to(RestSampler::class.java)
            .asEagerSingleton()
        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(AbstractRestSampler::class.java)
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
            .to(BlackBoxRestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(BlackBoxRestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<AbstractRestFitness>() {})
            .to(BlackBoxRestFitness::class.java)
            .asEagerSingleton()

        if(usingRemoteController) {
            bind(RemoteController::class.java)
                    .to(RemoteControllerImplementation::class.java)
                    .asEagerSingleton()
        }

        bind(object : TypeLiteral<Mutator<RestIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<RestIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(RestStructureMutator::class.java)
            .asEagerSingleton()

    }
}