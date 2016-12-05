package org.evomaster.core.search.onemax

import com.google.inject.*
import org.evomaster.core.search.FitnessFunction
import org.evomaster.core.search.Randomness
import org.evomaster.core.search.Sampler


class OneMaxModule : AbstractModule(){

    override fun configure() {
        bind(Key.get(object : TypeLiteral<Sampler<OneMaxIndividual>>() {}))
                .to(OneMaxSampler::class.java)
                .asEagerSingleton()

        bind(Key.get(object : TypeLiteral<FitnessFunction<OneMaxIndividual>>() {}))
                .to(OneMaxFitness::class.java)
                .asEagerSingleton()
    }

    @Provides @Singleton
    fun randomnessProvider() : Randomness {
        return Randomness(42)
    }
}