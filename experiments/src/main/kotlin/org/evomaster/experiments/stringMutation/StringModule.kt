package org.evomaster.experiments.stringMutation

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.EmptyStructureMutator
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * created by manzh on 2019-09-16
 */
class StringModule : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<StringIndividual>>() {})
                .to(StringSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<StringIndividual>>() {})
                .to(StringFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<StringIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<StringIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<StringIndividual>>() {})

        bind(object : TypeLiteral<Archive<StringIndividual>>() {})
                .asEagerSingleton()

        bind(StringProblemDefinition::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()
    }
}