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
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.search.algorithms.strategy.FixedSelectionStrategy
import org.evomaster.core.search.algorithms.observer.GARecorder

class StandardGeneticAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

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

            val solution = standardGeneticAlgorithm.search()

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }


    // Verifies that searchOnce() creates the next generation as expected
    @Test
    fun testNextGenerationIsElitesPlusSelectedOffspring() {
        TestUtils.handleFlaky {

            // Create GA with Fixed Selection
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            // Add Observer

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            // Set Config

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 1.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()

            val pop = ga.getViewOfPopulation()

            // Expected Elites (top-2 by combined fitness)
            val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)

            // Non Elites
            val expectedNonElites = pop.filter { it !in expectedElites }

            // Configure selection order after setup
            fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()

            // population size preserved
            assertEquals(config.populationSize, nextPop.size)

            // selection is called twice (via observer)
            assertEquals(2, rec.selections.size)

            // Check that elites are present in nextPop
            assertTrue(nextPop.any { it === expectedElites[0] })
            assertTrue(nextPop.any { it === expectedElites[1] })

            // crossover was called with 2 offspring (captured by observer)
            assertEquals(1, rec.xoCalls.size)
            val (o1, o2) = rec.xoCalls[0]
            assertTrue(nextPop.any { it === o1 })
            assertTrue(nextPop.any { it === o2 })

            // mutation happened twice on the offspring (captured by observer)
            assertEquals(2, rec.mutated.size)
            assertTrue(rec.mutated.any { it === o1 })
            assertTrue(rec.mutated.any { it === o2 })

            
        }
    }

     // Tests Edge Case: CrossoverProbability=0
    @Test
    fun testNoCrossoverWhenProbabilityZero() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()

                val pop = ga.getViewOfPopulation()
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)
                val expectedNonElites = pop.filter { it !in expectedElites }

                fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

                ga.searchOnce()

                val nextPop = ga.getViewOfPopulation()

                assertEquals(config.populationSize, nextPop.size)
                assertEquals(2, rec.selections.size)
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover disabled
                assertEquals(0, rec.xoCalls.size)
                // mutation still applied twice
                assertEquals(2, rec.mutated.size)
            
        }
    }

    // Tests Edge Case: MutationProbability=0
    @Test
    fun testNoMutationWhenProbabilityZero() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()

                val pop = ga.getViewOfPopulation()
                val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)
                val expectedNonElites = pop.filter { it !in expectedElites }

                fixedSel.setOrder(listOf(expectedNonElites[0], expectedNonElites[1]))

                ga.searchOnce()

                val nextPop = ga.getViewOfPopulation()

                assertEquals(config.populationSize, nextPop.size)
                assertEquals(2, rec.selections.size)
                assertTrue(nextPop.any { it === expectedElites[0] })
                assertTrue(nextPop.any { it === expectedElites[1] })

                // crossover forced
                assertEquals(1, rec.xoCalls.size)
                // mutation disabled
                assertEquals(0, rec.mutated.size)  
        }
    }
   
}

// --- Test helpers ---

private fun createGAWithSelection(
    fixedSel: FixedSelectionStrategy
): Pair<StandardGeneticAlgorithm<OneMaxIndividual>, Injector> {
    val injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    val ga = injector.getInstance(
        Key.get(object : TypeLiteral<StandardGeneticAlgorithm<OneMaxIndividual>>() {})
    )
    // Override selection strategy directly on the GA instance (no DI here)
    ga.setSelectionStrategy(fixedSel)
    return ga to injector
}