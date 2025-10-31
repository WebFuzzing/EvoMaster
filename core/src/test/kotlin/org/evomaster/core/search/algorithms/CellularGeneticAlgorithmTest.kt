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
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CellularGeneticAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that a single cGA iteration builds the next population from neighborhood winners.
    @Test
    fun testNextGenerationIsLocalWinnersFromNeighborhood() {
        TestUtils.handleFlaky {
            val cga = injector.getInstance(
                Key.get(
                    object : TypeLiteral<CellularGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            cga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 1.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.cgaNeighborhoodModel = EMConfig.CGANeighborhoodModel.RING

            cga.setupBeforeSearch()
            val initialPop = cga.getViewOfPopulation()

            cga.searchOnce()

            val nextPop = cga.getViewOfPopulation()
            val n = config.populationSize

            // population size preserved
            assertEquals(n, nextPop.size)

            // For each position i, next[i] must be either the original parent or one of the two offspring
            assertEquals(n, rec.xoCalls.size)
            for (i in 0 until n) {
                val (o1, o2) = rec.xoCalls[i]
                val p0 = initialPop[i]
                val chosen = nextPop[i]
                assertTrue(chosen === p0 || chosen === o1 || chosen === o2)
            }
        }
    }

    // Verifies that the Cellular GA can find the optimal solution for the OneMax problem
    @Test
    fun testCellularGeneticAlgorithmFindsOptimum() {
        TestUtils.handleFlaky {
            val cga = injector.getInstance(
                Key.get(
                    object : TypeLiteral<CellularGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = cga.search()
            epc.finishSearch()

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }
    
    // Edge Case: CrossoverProbability=0 and MutationProbability=1 on CGA
    @Test
    fun testNoCrossoverWhenProbabilityZero_CGA() {
        TestUtils.handleFlaky {
            val cga = injector.getInstance(
                Key.get(
                    object : TypeLiteral<CellularGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            cga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.cgaNeighborhoodModel = EMConfig.CGANeighborhoodModel.RING

            cga.setupBeforeSearch()
            cga.searchOnce()

            val nextPop = cga.getViewOfPopulation()
            val n = config.populationSize
            assertEquals(n, nextPop.size)
            // two selections per iteration
            assertEquals(2 * n, rec.selections.size)
            // crossover disabled
            assertEquals(0, rec.xoCalls.size)
            // should apply one mutations per iteration (mutation probability = 1)
            assertEquals(n, rec.mutated.size)
        }
    }

    // Edge Case: MutationProbability=0 and CrossoverProbability=1 on CGA
    @Test
    fun testNoMutationWhenProbabilityZero_CGA() {
        TestUtils.handleFlaky {
            val cga = injector.getInstance(
                Key.get(
                    object : TypeLiteral<CellularGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            cga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.cgaNeighborhoodModel = EMConfig.CGANeighborhoodModel.RING

            cga.setupBeforeSearch()
            cga.searchOnce()

            val nextPop = cga.getViewOfPopulation()
            val n = config.populationSize
            assertEquals(n, nextPop.size)
            // two selections per iteration
            assertEquals(2 * n, rec.selections.size)
            // one crossover per iteration (crossover probability = 1)
            assertEquals(n, rec.xoCalls.size) 
            // mutation disabled
            assertEquals(0, rec.mutated.size)
        }
    }
}
