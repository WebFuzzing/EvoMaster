package org.evomaster.core.problem.rest.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.fitness.ResourceRestFitness
import org.evomaster.core.problem.rest.service.fitness.RestFitness
import org.evomaster.core.problem.rest.service.mutator.ArazzoStructureMutator
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.rest.service.sampler.ArazzoSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

class ArazzoRestModule(private val bindRemote: Boolean = true) : RestBaseModule() {

    override fun configure() {

        super.configure()

        if (bindRemote) {
            bind(RemoteController::class.java)
                .to(RemoteControllerImplementation::class.java)
                .asEagerSingleton()
        }

        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
            .to(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<EnterpriseSampler<RestIndividual>>() {})
            .to(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<AbstractRestSampler>() {})
            .to(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(AbstractRestSampler::class.java)
            .to(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(ArazzoSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
            .to(RestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<AbstractRestFitness>() {})
            .to(ResourceRestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(RestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<RestIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<RestIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(ArazzoStructureMutator::class.java)
            .asEagerSingleton()

        bind(HttpWsExternalServiceHandler::class.java)
            .asEagerSingleton()

        bind(HarvestActualHttpWsResponseHandler::class.java)
            .asEagerSingleton()
    }
}
