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
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.search.algorithms.strategy.FixedSelectionStrategy
import org.evomaster.core.search.algorithms.strategy.SpyCrossoverOperator
import org.evomaster.core.search.algorithms.strategy.SpyMutationOperator

class StandardGeneticAlgorithmTest {

    val injector: Injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    // Verifies that the Standard GA can find the optimal solution for the OneMax problem
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
            if (epc.isInSearch()) {
                epc.finishSearch()
            }
            val solution = try {
            epc.startSearch()
                standardGeneticAlgorithm.search()
            } finally {
            epc.finishSearch()
            }

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }


    // Verifies that searchOnce() creates the next generation as expected
    @Test
    fun testNextGenerationIsElitesPlusSelectedOffspring() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val spyXo = SpyCrossoverOperator()
            val spyMut = SpyMutationOperator()

            val testModule = object : com.google.inject.AbstractModule() {
                override fun configure() {
                    bind(org.evomaster.core.search.algorithms.strategy.SelectionStrategy::class.java).toInstance(fixedSel)
                    bind(org.evomaster.core.search.algorithms.strategy.CrossoverOperator::class.java).toInstance(spyXo)
                    bind(org.evomaster.core.search.algorithms.strategy.MutationOperator::class.java).toInstance(spyMut)
                }
            }

            val overriddenBase: Module = com.google.inject.util.Modules.override(BaseModule()).with(testModule)

            val localInjector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(
                    OneMaxModule(),
                    overriddenBase
                ))
                .build().createInjector()

            val ga = localInjector.getInstance(
                Key.get(object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.apply {
                populationSize = 4
                elitesCount = 2
                xoverProbability = 1.0
                fixedRateMutation = 1.0
                gaSolutionSource = EMConfig.GASolutionSource.POPULATION
                maxEvaluations = 100_000
                stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            }

            if (epc.isInSearch()) epc.finishSearch()
            try {
                epc.startSearch()
                ga.setupBeforeSearch()

                val pop = ga.populationSnapshot()

                // Expected Elites (top-2 by combined fitness)
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)

                // Non Elites
                val expectedNonElites = pop.filter { it !in expectedElites }

                // Configure selection order after setup
                fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

                ga.searchOnce()

                val nextPop = ga.populationSnapshot()

                // population size preserved
                assertEquals(config.populationSize, nextPop.size)

                // selection is called twice
                assertEquals(2, fixedSel.getCallCount())

                // mutation is applied twice (once per selected parent)
                assertEquals(2, spyMut.getCallCount())

                // Check that elites are present in nextPop
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover was called with the 2 selected parents (in order)
                assertEquals(1, spyXo.calls.size)
                assertSame(expectedNonElites[0], spyXo.calls[0].first)
                assertSame(expectedNonElites[1], spyXo.calls[0].second)

                // mutation happened on the exact selected parents (identity, order-agnostic)
                assertEquals(2, spyMut.mutated.size)
                assertTrue(spyMut.mutated.any { it === expectedNonElites[0] })
                assertTrue(spyMut.mutated.any { it === expectedNonElites[1] })

            } finally {
                epc.finishSearch()
            }
        }
    }

    // Tests Edge Case: CrossoverProbability=0
    @Test
    fun testNoCrossoverWhenProbabilityZero() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val spyXo = SpyCrossoverOperator()
            val spyMut = SpyMutationOperator()

            val testModule = object : com.google.inject.AbstractModule() {
                override fun configure() {
                    bind(org.evomaster.core.search.algorithms.strategy.SelectionStrategy::class.java).toInstance(fixedSel)
                    bind(org.evomaster.core.search.algorithms.strategy.CrossoverOperator::class.java).toInstance(spyXo)
                    bind(org.evomaster.core.search.algorithms.strategy.MutationOperator::class.java).toInstance(spyMut)
                }
            }

            val overriddenBase: Module = com.google.inject.util.Modules.override(BaseModule()).with(testModule)

            val localInjector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(
                    OneMaxModule(),
                    overriddenBase
                ))
                .build().createInjector()

            val ga = localInjector.getInstance(
                Key.get(object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.apply {
                populationSize = 4
                elitesCount = 2
                xoverProbability = 0.0 // disable crossover
                fixedRateMutation = 1.0 // force mutation
                gaSolutionSource = EMConfig.GASolutionSource.POPULATION
                maxEvaluations = 100_000
                stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            }

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
                assertEquals(2, fixedSel.getCallCount())
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover disabled
                assertEquals(0, spyXo.calls.size)
                // mutation still applied twice
                assertEquals(2, spyMut.getCallCount())
            } finally {
                epc.finishSearch()
            }
        }
    }

    // Tests Edge Case: MutationProbability=0
    @Test
    fun testNoMutationWhenProbabilityZero() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val spyXo = SpyCrossoverOperator()
            val spyMut = SpyMutationOperator()

            val testModule = object : com.google.inject.AbstractModule() {
                override fun configure() {
                    bind(org.evomaster.core.search.algorithms.strategy.SelectionStrategy::class.java).toInstance(fixedSel)
                    bind(org.evomaster.core.search.algorithms.strategy.CrossoverOperator::class.java).toInstance(spyXo)
                    bind(org.evomaster.core.search.algorithms.strategy.MutationOperator::class.java).toInstance(spyMut)
                }
            }

            val overriddenBase: Module = com.google.inject.util.Modules.override(BaseModule()).with(testModule)

            val localInjector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(
                    OneMaxModule(),
                    overriddenBase
                ))
                .build().createInjector()

            val ga = localInjector.getInstance(
                Key.get(object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = localInjector.getInstance(EMConfig::class.java)
            val epc = localInjector.getInstance(ExecutionPhaseController::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.apply {
                populationSize = 4
                elitesCount = 2
                xoverProbability = 1.0 // force crossover
                fixedRateMutation = 0.0 // disable mutation
                gaSolutionSource = EMConfig.GASolutionSource.POPULATION
                maxEvaluations = 100_000
                stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            }

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
                assertEquals(2, fixedSel.getCallCount())
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover forced
                assertEquals(1, spyXo.calls.size)
                // mutation disabled
                assertEquals(0, spyMut.getCallCount())
            } finally {
            epc.finishSearch()
            }
        }
    }

}