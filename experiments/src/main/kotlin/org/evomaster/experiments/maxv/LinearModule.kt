package org.evomaster.experiments.maxv

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.CombinedMutator
import org.evomaster.core.search.mutator.RandomMutator
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Mutator
import org.evomaster.core.search.service.Sampler


class LinearModule : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<LinearIndividual>>() {})
                .to(LinearSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<LinearIndividual>>() {})
                .to(LinearFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<LinearIndividual>>() {})
                .to(object : TypeLiteral<RandomMutator<LinearIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<LinearIndividual>>() {})
                .asEagerSingleton()


        bind(object : TypeLiteral<RandomMutator<LinearIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<StandardMutator<LinearIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<CombinedMutator<LinearIndividual>>() {})
                .asEagerSingleton()


        bind(LinearProblemDefinition::class.java)
                .asEagerSingleton()
    }
}