package org.evomaster.core.search.algorithms.onemax

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.service.mutator.EmptyStructureMutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
import org.evomaster.core.search.tracer.TrackService


class OneMaxModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<OneMaxIndividual>>() {})
                .to(OneMaxSampler::class.java)
                .asEagerSingleton()

        bind(OneMaxSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<OneMaxIndividual>>() {})
                .to(OneMaxFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<OneMaxIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<OneMaxIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<OneMaxIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<OneMaxIndividual>>() {})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()

        bind(TrackService::class.java)
                .to(ArchiveMutationTrackService::class.java)
                .asEagerSingleton()

        bind(ArchiveMutationTrackService::class.java)
                .asEagerSingleton()

    }
}