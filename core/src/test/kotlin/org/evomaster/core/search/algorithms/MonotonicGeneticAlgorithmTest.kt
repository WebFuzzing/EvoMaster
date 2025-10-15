package org.evomaster.core.search.algorithms

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.algorithms.observer.GARecorder
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.algorithms.strategy.FixedSelectionStrategy
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MonotonicGeneticAlgorithmTest {

    val injector: Injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    // Verifies that the Monotonic GA can find the optimal solution for the OneMax problem
    @Test
    fun testMonotonicGeneticAlgorithmFindsOptimum() {
        TestUtils.handleFlaky {
            val monoGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<MonotonicGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            if (epc.isInSearch()) {
                epc.finishSearch()
            }

            val solution = try {
                epc.startSearch()
                monoGA.search()
            } finally {
                epc.finishSearch()
            }

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

    // Ensures that maximum fitness never decreases across generations when running full search
    @Test
    fun testMonotonicReplacementRule() {
        TestUtils.handleFlaky {
            val monoGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<MonotonicGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            monoGA.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 100
            config.elitesCount = 4
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            if (epc.isInSearch()) epc.finishSearch()
            val solution = try {
                epc.startSearch()
                monoGA.search()
            } finally {
                epc.finishSearch()
            }

            // Check monotonicity across recorded generations: best score (selection metric) is non-decreasing
            val bestScores = rec.bestFitnessPerGeneration
            for (k in 1 until bestScores.size) {
                assertTrue(bestScores[k] >= bestScores[k-1])
            }
        }
    }
}

// --- Test helpers ---

private fun createMonotonicGAWithSelection(
    fixedSel: FixedSelectionStrategy
): Pair<MonotonicGeneticAlgorithm<OneMaxIndividual>, Injector> {
    val testModule = object : com.google.inject.AbstractModule() {
        override fun configure() {
            bind(org.evomaster.core.search.algorithms.strategy.SelectionStrategy::class.java)
                .toInstance(fixedSel)
        }
    }

    val injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(
            OneMaxModule(),
            com.google.inject.util.Modules.override(BaseModule()).with(testModule)
        ))
        .build().createInjector()

    val ga = injector.getInstance(
        Key.get(object : TypeLiteral<MonotonicGeneticAlgorithm<OneMaxIndividual>>() {})
    )
    return ga to injector
}


