package org.evomaster.experiments.linear

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.EmptyStructureMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*


class LinearModule : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<LinearIndividual>>() {})
                .to(LinearSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<LinearIndividual>>() {})
                .to(LinearFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<LinearIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<LinearIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<LinearIndividual>>() {})
                .asEagerSingleton()


        bind(LinearProblemDefinition::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(EmptyStructureMutator::class.java)
                .asEagerSingleton()
    }
}