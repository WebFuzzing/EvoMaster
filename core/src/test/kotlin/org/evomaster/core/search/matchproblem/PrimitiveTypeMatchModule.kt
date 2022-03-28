package org.evomaster.core.search.matchproblem

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.NoTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.*

/**
 * created by manzh on 2020-06-16
 */
class PrimitiveTypeMatchModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(PrimitiveTypeMatchSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<PrimitiveTypeMatchIndividual>>() {})
                .to(PrimitiveTypeMatchSampler::class.java)
                .asEagerSingleton()

        bind(PrimitiveTypeMatchSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<PrimitiveTypeMatchIndividual>>() {})
                .to(PrimitiveTypeMatchFitness::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<Mutator<PrimitiveTypeMatchIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<PrimitiveTypeMatchIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<PrimitiveTypeMatchIndividual>>() {})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()

        bind(TestCaseWriter::class.java)
                .to(NoTestCaseWriter::class.java)
                .asEagerSingleton()
    }
}