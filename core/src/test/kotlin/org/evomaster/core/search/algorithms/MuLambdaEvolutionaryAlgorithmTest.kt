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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MuLambdaEvolutionaryAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that the (μ,λ) EA can find the optimal solution for the OneMax problem
    @Test
    fun testMuLambdaEAFindsOptimum() {
        TestUtils.handleFlaky {
            val ea = injector.getInstance(
                Key.get(object : TypeLiteral<MuLambdaEvolutionaryAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.populationSize = 5
            config.muLambdaOffspringSize = 10
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = ea.search()
            epc.finishSearch()

            assertEquals(1, solution.individuals.size)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

    // Edge Case: CrossoverProbability=0 and MutationProbability=1
    @Test
    fun testNoCrossoverWhenProbabilityZero_MuLambdaEA() {
        TestUtils.handleFlaky {
            val ea = injector.getInstance(
                Key.get(object : TypeLiteral<MuLambdaEvolutionaryAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ea.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.populationSize = 5
            config.muLambdaOffspringSize = 10 // divisible by mu
            config.xoverProbability = 0.0 // no crossover used in (μ,λ)
            config.fixedRateMutation = 1.0 // force mutation

            ea.setupBeforeSearch()
            ea.searchOnce()

            val nextPop = ea.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)

            // crossover unused
            assertEquals(0, rec.xoCalls.size)
            // offspring mutated: perParent * µ == λ when divisible
            val perParent = config.muLambdaOffspringSize / config.populationSize
            assertEquals(perParent * config.populationSize, rec.mutated.size)
        }
    }

    // Edge Case: MutationProbability=0 and CrossoverProbability=1
    @Test
    fun testNoMutationWhenProbabilityZero_MuLambdaEA() {
        TestUtils.handleFlaky {
            val ea = injector.getInstance(
                Key.get(object : TypeLiteral<MuLambdaEvolutionaryAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ea.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.populationSize = 5
            config.muLambdaOffspringSize = 10
            config.xoverProbability = 1.0 // irrelevant for (μ,λ)
            config.fixedRateMutation = 0.0 // disable mutation

            ea.setupBeforeSearch()
            ea.searchOnce()

            val nextPop = ea.getViewOfPopulation()
            assertEquals(config.populationSize, nextPop.size)
            assertEquals(0, rec.xoCalls.size)
            assertEquals(0, rec.mutated.size)
        }
    }

    // One iteration properties: population size, best-µ selection from offspring, mutation count
    @Test
    fun testNextGenerationIsTheBestMuFromOffspringOnly() {
        TestUtils.handleFlaky {
            val ea = injector.getInstance(
                Key.get(object : TypeLiteral<MuLambdaEvolutionaryAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ea.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.populationSize = 5
            config.muLambdaOffspringSize = 10 // divisible by mu -> perParent = 2
            config.xoverProbability = 0.0
            config.fixedRateMutation = 1.0

            ea.setupBeforeSearch()
            ea.searchOnce()

            val finalPop = ea.getViewOfPopulation()
            val mu = config.populationSize

            // 1) population size remains µ
            assertEquals(mu, finalPop.size)

            // 2) final population equals best-µ from offspring only
            val offspring = rec.mutated.toList()
            val expectedScores = offspring
                .map { ea.score(it) }
                .sortedDescending()
                .take(mu)
            val finalScores = finalPop
                .map { ea.score(it) }
                .sortedDescending()
            assertEquals(expectedScores, finalScores)

            // 3) mutation count equals number of created offspring
            val perParent = config.muLambdaOffspringSize / config.populationSize
            assertEquals(perParent * config.populationSize, rec.mutated.size)
        }
    }
}


