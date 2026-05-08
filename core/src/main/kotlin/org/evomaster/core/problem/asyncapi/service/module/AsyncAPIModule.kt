package org.evomaster.core.problem.asyncapi.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.asyncapi.service.fitness.AsyncAPIFitness
import org.evomaster.core.problem.asyncapi.service.sampler.AsyncAPISampler
import org.evomaster.core.problem.asyncapi.service.structure.AsyncAPIStructureMutator
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * White-box AsyncAPI Guice bindings.
 *
 * Mirrors [AsyncAPIBlackBoxModule][org.evomaster.core.problem.asyncapi.service.module.AsyncAPIBlackBoxModule]
 * but binds [AsyncAPIFitness] (which polls the EM Driver for coverage) and
 * a real [RemoteController] so the driver connection is available for
 * `registerNewAction` / `getTestResults` calls.
 */
class AsyncAPIModule : AsyncAPIBaseModule() {

    override fun configure() {
        super.configure()

        bind(AsyncAPISampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<EnterpriseSampler<AsyncAPIIndividual>>() {})
            .to(AsyncAPISampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<AsyncAPIIndividual>>() {})
            .to(AsyncAPISampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(AsyncAPISampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<AsyncAPIIndividual>>() {})
            .to(AsyncAPIFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(AsyncAPIFitness::class.java)
            .asEagerSingleton()

        bind(RemoteController::class.java)
            .to(RemoteControllerImplementation::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<AsyncAPIIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(AsyncAPIStructureMutator::class.java)
            .asEagerSingleton()
    }
}
