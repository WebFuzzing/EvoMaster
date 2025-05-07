package org.evomaster.core.problem.rest.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.fitness.ResourceRestFitness
import org.evomaster.core.problem.rest.service.mutator.ResourceRestMutator
import org.evomaster.core.problem.rest.service.mutator.ResourceRestStructureMutator
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator


class ResourceRestModule(private val bindRemote : Boolean = true) : RestBaseModule(){

    override fun configure() {

        super.configure()

        if(bindRemote){
            /*
                Governator does not seem to have a way to override bindings for testing :(
                so we do it manually
             */
            bind(RemoteController::class.java)
                    .to(RemoteControllerImplementation::class.java)
                    .asEagerSingleton()
        }

        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()
        bind(object : TypeLiteral<AbstractRestSampler>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(AbstractRestSampler::class.java)
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
                .to(ResourceRestFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
                .to(ResourceRestFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<AbstractRestFitness>() {})
                .to(ResourceRestFitness::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<Mutator<RestIndividual>>() {})
                .to(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StandardMutator<RestIndividual>>() {})
                .to(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(ResourceRestStructureMutator::class.java)
                .asEagerSingleton()

        bind(ResourceManageService::class.java)
                .asEagerSingleton()

        bind(ResourceDepManageService::class.java)
                .asEagerSingleton()


        bind(HttpWsExternalServiceHandler::class.java)
                .asEagerSingleton()

        bind(HarvestActualHttpWsResponseHandler::class.java)
            .asEagerSingleton()



    }
}