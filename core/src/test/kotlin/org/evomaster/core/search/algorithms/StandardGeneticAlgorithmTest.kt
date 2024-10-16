package org.evomaster.core.search.algorithms

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class StandardGeneticAlgorithmTest {

    val injector: Injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    @Test
    fun search() {
        TestUtils.handleFlaky {
            val standardGeneticAlgorithm = injector.getInstance(
                Key.get(
                object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {}))

            val config = injector.getInstance(EMConfig::class.java)
            config.maxActionEvaluations = 30000
            config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

            val solution = standardGeneticAlgorithm.search()
            println("size: ${solution.individuals.size}")
//            assertEquals(3.0, solution.overall.computeFitnessScore(), 0.001)
            //assertTrue(solution.individuals.size <= 2)
        }
    }
}