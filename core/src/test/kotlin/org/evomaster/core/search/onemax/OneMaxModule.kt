package org.evomaster.core.search.onemax

import com.google.inject.*
import org.evomaster.core.EMConfig
import org.evomaster.core.search.*
import org.evomaster.core.search.mutator.Mutator
import org.evomaster.core.search.mutator.RandomMutator


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
                .to(object : TypeLiteral<RandomMutator<OneMaxIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<OneMaxIndividual>>() {})
            .asEagerSingleton()

        //TODO this will need refactoring

        bind(EMConfig::class.java)
            .asEagerSingleton()

        bind(SearchTimeController::class.java)
            .asEagerSingleton()

        bind(AdaptiveParameterControl::class.java)
            .asEagerSingleton()
    }

    @Provides @Singleton
    fun randomnessProvider() : Randomness {
        return Randomness(42)
    }
}