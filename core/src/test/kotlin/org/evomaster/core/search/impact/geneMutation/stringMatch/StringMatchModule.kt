package org.evomaster.core.search.impact.geneMutation.stringMatch

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.*

/**
 * created by manzh on 2020-06-16
 */
class StringMatchModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<StringMatchIndividual>>() {})
                .to(StringMatchSampler::class.java)
                .asEagerSingleton()

        bind(StringMatchSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<StringMatchIndividual>>() {})
                .to(StringMatchFitness::class.java)
                .asEagerSingleton()


        bind(object : TypeLiteral<Mutator<StringMatchIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<StringMatchIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<StringMatchIndividual>>() {})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()
    }
}