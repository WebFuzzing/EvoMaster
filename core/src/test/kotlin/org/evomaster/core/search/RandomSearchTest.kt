package org.evomaster.core.search

import com.google.inject.*
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.onemax.OneMaxFitness
import org.evomaster.core.search.onemax.OneMaxIndividual
import org.evomaster.core.search.onemax.OneMaxSampler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class RandomSearchTest {

    class RandomOneMax : AbstractModule(){
        override fun configure() {
            bind(Key.get(object : TypeLiteral<Sampler<OneMaxIndividual>>() {}))
                    .to(OneMaxSampler::class.java)

            bind(Key.get(object : TypeLiteral<FitnessFunction<OneMaxIndividual>>() {}))
                    .to(OneMaxFitness::class.java)
        }

        @Provides @Singleton
        fun randomnessProvider() : Randomness{
            return Randomness(42)
        }
    }

    val injector: Injector = Guice.createInjector(* arrayOf<Module>(RandomOneMax()))

    @Test
    fun testRandomSearch(){

        val rs = injector.getInstance(Key.get(
                object : TypeLiteral<RandomAlgorithm<OneMaxIndividual>>() {}))

        val solution = rs.search(1000)

        assertEquals(3.0, solution.overall.computeFitnessScore(), 0.001);
        assertEquals(1, solution.individuals.size)
    }
}