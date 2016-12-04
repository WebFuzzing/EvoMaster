package org.evomaster.core.search

import com.google.inject.*
import org.evomaster.core.search.onemax.OneMaxFitness
import org.evomaster.core.search.onemax.OneMaxIndividual
import org.evomaster.core.search.onemax.OneMaxSampler
import org.junit.jupiter.api.Test


class RandomSearchTest {

    class RandomOneMax : AbstractModule(){
        override fun configure() {
        }

        @Provides @Singleton
        fun randomnessProvider() : Randomness{
            return Randomness(42)
        }

        @Provides
        fun samplerProvider() : Sampler<OneMaxIndividual>{
            return OneMaxSampler()
        }

        @Provides
        fun fitnessFunctionProvider() : FitnessFunction<OneMaxIndividual>{
            return OneMaxFitness()
        }
    }

    val modules = arrayOf<com.google.inject.Module>(RandomOneMax())
    //val injector = Guice::createInjector(*modules)

    @Test
    fun testRandomSearch(){

    }
}