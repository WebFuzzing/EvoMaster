package org.evomaster.core.search

import com.google.inject.*
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.onemax.OneMaxIndividual
import org.evomaster.core.search.onemax.OneMaxModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class RandomSearchTest {

    val injector: Injector = Guice.createInjector(* arrayOf<Module>(
            OneMaxModule(), BaseModule()))

    @Test
    fun testRandomSearch(){

        val rs = injector.getInstance(Key.get(
                object : TypeLiteral<RandomAlgorithm<OneMaxIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)
        config.maxFitnessEvaluations = 1000

        val solution = rs.search()

        assertEquals(3.0, solution.overall.computeFitnessScore(), 0.001);
        assertEquals(1, solution.individuals.size)
    }
}