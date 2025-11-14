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
import org.evomaster.core.search.algorithms.observer.GARecorder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LIPSAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that the LIPS algorithm can find the optimal solution for the OneMax problem
    @Test
    fun testLipsFindsOptimumOnOneMax() {
        
        val lips = injector.getInstance(
            Key.get(object : TypeLiteral<LIPSAlgorithm<OneMaxIndividual>>() {})
        )

        val config = injector.getInstance(EMConfig::class.java)
        config.maxEvaluations = 10_000
        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

        val epc = injector.getInstance(ExecutionPhaseController::class.java)
        epc.startSearch()
        val solution = lips.search()
        epc.finishSearch()

        assertTrue(solution.individuals.size == 1)
        assertEquals(
            OneMaxSampler.DEFAULT_N.toDouble(),
            solution.overall.computeFitnessScore(),
            0.001
        )
    }
    
    // Edge Case: CrossoverProbability=0 on LIPS
    @Test
    fun testNoCrossoverWhenProbabilityZero_LIPS() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<LIPSAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.populationSize = 5
            config.xoverProbability = 0.0 // no crossover
            config.fixedRateMutation = 1.0 // force mutation

            ga.setupBeforeSearch()
            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)
            assertEquals(0, rec.xoCalls.size)
            // at least the offspring should have been mutated
            assertTrue(rec.mutated.size >= 1)
        }
    }

    // Edge Case: MutationProbability=0 on LIPS
    @Test
    fun testNoMutationWhenProbabilityZero_LIPS() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<LIPSAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.populationSize = 5
            config.xoverProbability = 1.0 // allow crossover
            config.fixedRateMutation = 0.0 // disable mutation

            ga.setupBeforeSearch()
            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)
            assertEquals(0, rec.mutated.size)
        }
    }

    // Next generation is elites + best from offspring
    @Test
    fun testNextGenerationIsElitesPlusBestFromOffspring_LIPS() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<LIPSAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.populationSize = 5
            config.elitesCount = 1
            config.xoverProbability = 0.0
            config.fixedRateMutation = 1.0
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            ga.setupBeforeSearch()

            val initPop = ga.getViewOfPopulation()
            val eliteCount = config.elitesCount
            val mu = config.populationSize

            // Determine expected elites from current population
            val eliteScores = initPop
                .map { ga.score(it) }
                .sortedDescending()
                .take(eliteCount)

            ga.searchOnce()

            val finalPop = ga.getViewOfPopulation()

            // population size remains the same
            assertEquals(mu, finalPop.size)

            // offspring considered as those mutated
            val offspringScores = rec.mutated
                .map { ga.score(it) }
                .sortedDescending()

            val neededFromOffspring = mu - eliteCount
            val expectedScores = (eliteScores + offspringScores.take(neededFromOffspring))
                .sortedDescending()

            val finalScores = finalPop
                .map { ga.score(it) }
                .sortedDescending()

            assertEquals(expectedScores, finalScores)
            // All non-elite positions were filled by mutated offspring
            assertEquals(neededFromOffspring, rec.mutated.size)
        }
    }
    
}


