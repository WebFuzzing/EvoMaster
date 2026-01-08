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
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class BreederGeneticAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that the Breeder GA can find the optimal solution for the OneMax problem
    @Test
    fun testBreederGeneticAlgorithmFindsOptimum() {
        TestUtils.handleFlaky {
            val breederGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<BreederGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = breederGA.search()
            epc.finishSearch()

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

    // Verifies that BGA forms next generation as elites + chosen children from truncation
    @Test
    fun testNextGenerationIsElitesPlusTruncationChildren() {
        TestUtils.handleFlaky {
            val breederGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<BreederGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            breederGA.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(42)

            config.populationSize = 4
            config.elitesCount = 2
            config.xoverProbability = 1.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            breederGA.setupBeforeSearch()

            val pop = breederGA.getViewOfPopulation()
            val expectedElites = pop.sortedByDescending { it.calculateCombinedFitness() }.take(2)

            breederGA.searchOnce()

            val nextPop = breederGA.getViewOfPopulation()

            // population size preserved
            assertEquals(config.populationSize, nextPop.size)

            // elites are present in next population
            assertTrue(nextPop.any { it === expectedElites[0] })
            assertTrue(nextPop.any { it === expectedElites[1] })

            // number of iterations equals children added = populationSize - elites
            val iterations = config.populationSize - config.elitesCount
            assertEquals(iterations, rec.xoCalls.size)

            // each iteration produced (o1,o2); exactly one should be carried over
            rec.xoCalls.forEach { (o1, o2) ->
                assertTrue(nextPop.any { it === o1 } || nextPop.any { it === o2 })
            }

            // two mutations per iteration (one per offspring)
            assertEquals(2 * iterations, rec.mutated.size)
        }
    }

    // Edge Case: CrossoverProbability=0 and MutationProbability=1 on BGA
    @Test
    fun testNoCrossoverWhenProbabilityZero_BGA() {
        TestUtils.handleFlaky {
            val breederGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<BreederGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            breederGA.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.populationSize = 4
            config.elitesCount = 0
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            breederGA.setupBeforeSearch()
            breederGA.searchOnce()

            val nextPop = breederGA.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)

            // crossover disabled
            assertEquals(0, rec.xoCalls.size)
            // should apply two mutations per iteration (mutation probability = 1)
            assertEquals(2 * config.populationSize, rec.mutated.size)
        }
    }

    // Edge Case: MutationProbability=0 and CrossoverProbability=1 on BGA
    @Test
    fun testNoMutationWhenProbabilityZero_BGA() {
        TestUtils.handleFlaky {
            val breederGA = injector.getInstance(
                Key.get(
                    object : TypeLiteral<BreederGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            breederGA.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.populationSize = 4
            config.elitesCount = 0
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            breederGA.setupBeforeSearch()
            breederGA.searchOnce()

            val nextPop = breederGA.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)

            // crossovers happen once per iteration (mutation probability = 1) 
            assertEquals(config.populationSize, rec.xoCalls.size)

            // mutations disabled
            assertEquals(0, rec.mutated.size)
        }
    }

    
}
