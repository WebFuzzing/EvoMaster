package org.evomaster.core.search.onemax

import com.google.inject.*
import org.evomaster.core.search.FitnessFunction
import org.evomaster.core.search.Randomness
import org.evomaster.core.search.Sampler
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
    }

    @Provides @Singleton
    fun randomnessProvider() : Randomness {
        return Randomness(42)
    }
}