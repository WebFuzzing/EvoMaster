package org.evomaster.core.problem.asyncapi.service.module

import com.google.inject.TypeLiteral
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.asyncapi.service.fitness.AsyncAPIBlackBoxFitness
import org.evomaster.core.problem.asyncapi.service.sampler.AsyncAPISampler
import org.evomaster.core.problem.asyncapi.service.structure.AsyncAPIStructureMutator
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * Guice bindings for AsyncAPI black-box.  Mirrors `BlackBoxRestModule` and
 * `GraphQLBlackBoxModule` — same shape, different classes.
 *
 * The white-box variant (M5) will share [AsyncAPIBaseModule] but bind a
 * coverage-collecting fitness instead of [AsyncAPIBlackBoxFitness].
 */
class AsyncAPIBlackBoxModule : AsyncAPIBaseModule() {

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
            .to(AsyncAPIBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(AsyncAPIBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<AsyncAPIIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(AsyncAPIStructureMutator::class.java)
            .asEagerSingleton()
    }
}
