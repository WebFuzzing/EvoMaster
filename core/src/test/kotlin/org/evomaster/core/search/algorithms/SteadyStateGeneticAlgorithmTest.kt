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
import org.evomaster.core.search.algorithms.observer.GARecorder
import org.evomaster.core.search.algorithms.strategy.FixedSelectionStrategy
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SteadyStateGeneticAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that the Steady-State GA can find the optimal solution for the OneMax problem
    @Test
    fun testSteadyStateAlgorithm() {
        TestUtils.handleFlaky {
            val steadyStateAlgorithm = injector.getInstance(
                Key.get(
                    object : TypeLiteral<SteadyStateGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = steadyStateAlgorithm.search()
            epc.finishSearch()
            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

     // Verifies steady-state replacement: replace parents only if children are better
    @Test
    fun testSteadyStateReplacementIfChildrenBetter() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 1.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()

            val pop = ga.getViewOfPopulation()

            // Select 2 deterministic parents from the initial population
            val p1 = pop[0]
            val p2 = pop[1]
            fixedSel.setOrder(listOf(p1, p2))

            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()

            // Size preserved
            assertEquals(config.populationSize, nextPop.size)

            // Exactly two selections
            assertEquals(2, rec.selections.size)

            // Crossover was called, capture offspring
            assertEquals(1, rec.xoCalls.size)
            val (o1, o2) = rec.xoCalls[0]

            // Replacement rule: keep offspring only if better than parents
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
        }
    }

    // Edge Case: CrossoverProbability=0 on SSGA
    @Test
    fun testNoCrossoverWhenProbabilityZero_SSGA() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()
            // Provide a deterministic selection order for the 2 selections in SSGA
            val init = ga.getViewOfPopulation()
            fixedSel.setOrder(listOf(init[0], init[1]))
            ga.searchOnce()

            // population size preserved
            val nextPop = ga.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)

            // exactly two selections in one steady-state step
            assertEquals(2, rec.selections.size)
            // crossover disabled
            assertEquals(0, rec.xoCalls.size)
            // two mutations (one per offspring)
            assertEquals(2, rec.mutated.size)
        }
    }

    // Edge Case: MutationProbability=0 on SSGA
    @Test
    fun testNoMutationWhenProbabilityZero_SSGA() {
        TestUtils.handleFlaky {
            val fixedSel = FixedSelectionStrategy()
            val (ga, localInjector) = createGAWithSelection(fixedSel)

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = localInjector.getInstance(EMConfig::class.java)
            localInjector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()
            val init = ga.getViewOfPopulation()
            fixedSel.setOrder(listOf(init[0], init[1]))
            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)

            // two selections, one crossover, zero mutations
            assertEquals(2, rec.selections.size)
            assertEquals(1, rec.xoCalls.size)
            assertEquals(0, rec.mutated.size)
        }
    }



}

// --- Test helpers ---

// --- Test helpers ---

private fun createGAWithSelection(
    fixedSel: FixedSelectionStrategy
): Pair<SteadyStateGeneticAlgorithm<OneMaxIndividual>, Injector> {
    val injector = LifecycleInjector.builder()
        .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
        .build().createInjector()

    val ga = injector.getInstance(
        Key.get(object : TypeLiteral<SteadyStateGeneticAlgorithm<OneMaxIndividual>>() {})
    )
    // Override selection strategy directly on the GA instance (no DI here)
    ga.useSelectionStrategy(fixedSel)
    return ga to injector
}




