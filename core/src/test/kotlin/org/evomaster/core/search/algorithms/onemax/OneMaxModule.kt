package org.evomaster.core.search.algorithms.onemax

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Mutator
import org.evomaster.core.search.service.Sampler


class OneMaxModule : AbstractModule(){

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

    }
}