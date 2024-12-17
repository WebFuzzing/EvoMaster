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
import org.evomaster.core.search.service.ExecutionPhaseController
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class StandardGeneticAlgorithmTest {

    val injector: Injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    @Test
    fun testStandardGeneticAlgorithm() {
        TestUtils.handleFlaky {
            val standardGeneticAlgorithm = injector.getInstance(
                Key.get(
                    object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()

            val solution = standardGeneticAlgorithm.search()

            epc.finishSearch()

            assertTrue(solution.individuals.size == 1)
            assertEquals(3.0, solution.overall.computeFitnessScore(), 0.001)
        }
    }

}