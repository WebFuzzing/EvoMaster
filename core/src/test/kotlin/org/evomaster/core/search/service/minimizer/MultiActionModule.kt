package org.evomaster.core.search.service.minimizer

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.NoTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.mutator.EmptyStructureMutator
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.search.tracer.ArchiveMutationTrackService
import org.evomaster.core.search.tracer.TrackService

class MultiActionModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(MultiActionSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<MultiActionIndividual>>() {})
            .to(MultiActionSampler::class.java)
            .asEagerSingleton()

        bind(MultiActionSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<MultiActionIndividual>>() {})
            .to(MultiActionFitness::class.java)
            .asEagerSingleton()

        bind(MultiActionFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<MultiActionIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<MultiActionIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<MultiActionIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<MultiActionIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<MultiActionIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(EmptyStructureMutator::class.java)
            .asEagerSingleton()

        bind(TrackService::class.java)
            .to(ArchiveMutationTrackService::class.java)
            .asEagerSingleton()

        bind(ArchiveMutationTrackService::class.java)
            .asEagerSingleton()

        bind(TestCaseWriter::class.java)
            .to(NoTestCaseWriter::class.java)
            .asEagerSingleton()
    }
}
