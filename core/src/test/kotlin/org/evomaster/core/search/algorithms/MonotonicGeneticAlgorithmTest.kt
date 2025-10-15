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

    // Tests Edge Case: CrossoverProbability=0 on Monotonic GA
    @Test
    fun testNoCrossoverWhenProbabilityZero_Monotonic() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createMonotonicGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            if (epc.isInSearch()) epc.finishSearch()
            try {
                epc.startSearch()
                ga.setupBeforeSearch()

                val pop = ga.populationSnapshot()
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)
                val expectedNonElites = pop.filter { it !in expectedElites }

                fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

                ga.searchOnce()

                val nextPop = ga.populationSnapshot()

                assertEquals(config.populationSize, nextPop.size)
                assertEquals(2, rec.selections.size)
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover disabled
                assertEquals(0, rec.xoCalls.size)
                // mutation still applied twice
                assertEquals(2, rec.mutated.size)
            } finally {
                epc.finishSearch()
            }
        }
    }

    // Tests Edge Case: MutationProbability=0 on Monotonic GA
    @Test
    fun testNoMutationWhenProbabilityZero_Monotonic() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createMonotonicGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            if (epc.isInSearch()) epc.finishSearch()
            try {
                epc.startSearch()
                ga.setupBeforeSearch()

                val pop = ga.populationSnapshot()
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)
                val expectedNonElites = pop.filter { it !in expectedElites }

                fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

                ga.searchOnce()

                val nextPop = ga.populationSnapshot()

                assertEquals(config.populationSize, nextPop.size)
                assertEquals(2, rec.selections.size)
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover forced
                assertEquals(1, rec.xoCalls.size)
                // mutation disabled
                assertEquals(0, rec.mutated.size)
            } finally {
                epc.finishSearch()
            }
        }
    }
    // Verifies that one generation is formed by elites plus monotonic replacement outcome
    @Test
    fun testNextGenerationElitesPlusMonotonicReplacement() {
        TestUtils.handleFlaky {
            // Fixed selection for determinism
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createMonotonicGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 1.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            if (epc.isInSearch()) epc.finishSearch()
            try {
                epc.startSearch()
                ga.setupBeforeSearch()

                val pop = ga.populationSnapshot()

                // Elites by combined fitness
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)
                val expectedNonElites = pop.filter { it !in expectedElites }

                // Force selection of non-elites
                val p1 = expectedNonElites[0]
                val p2 = expectedNonElites[1]
                fixedSel.setOrder(listOf(p1, p2))

                ga.searchOnce()

                val nextPop = ga.populationSnapshot()

                // Size preserved
                assertEquals(config.populationSize, nextPop.size)
                // Elites carried over
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })
                // Selections counted via observer
                assertEquals(2, rec.selections.size)

                // Offspring seen via crossover observer
                assertEquals(1, rec.xoCalls.size)
                val (o1, o2) = rec.xoCalls[0]

                // Monotonic rule: keep offspring only if better than parents according to GA score
                val parentBest = kotlin.math.max(ga.score(p1), ga.score(p2))
                val childBest = kotlin.math.max(ga.score(o1), ga.score(o2))

                if (childBest > parentBest) {
                    assertTrue(nextPop.any { it === o1 })
                    assertTrue(nextPop.any { it === o2 })
                    assertFalse(nextPop.containsAll(listOf(p1, p2)))
                } else {
                    assertTrue(nextPop.any { it === p1 })
                    assertTrue(nextPop.any { it === p2 })
                    assertFalse(nextPop.containsAll(listOf(o1, o2)))
                }

                // Mutation applied twice to offspring
                assertEquals(2, rec.mutated.size)
                assertTrue(rec.mutated.any { it === o1 })
                assertTrue(rec.mutated.any { it === o2 })
            } finally {
                epc.finishSearch()
            }
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


