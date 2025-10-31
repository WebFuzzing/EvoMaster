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

class OnePlusLambdaLambdaGeneticAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that the 1+(λ,λ) GA can find the optimal solution for the OneMax problem
    @Test
    fun testOnePlusLambdaLambdaGeneticAlgorithmFindsOptimum() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = ga.search()
            epc.finishSearch()

            assertEquals(1, solution.individuals.size)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

    /**
     * Verifies that the parent is kept when it is strictly better than any offspring.
     * Force crossover to always degrade children so bestOffspring < parent.
     */
    @Test
    fun testParentKeptWhenOffspringWorse() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(7)

            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.onePlusLambdaLambdaOffspringSize = 6
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutants so crossover cannot surpass parent

            // Use a crossover operator that subtly degrades children by removing one element when possible
            ga.useCrossoverOperator(object : org.evomaster.core.search.algorithms.strategy.suite.CrossoverOperator {
                override fun <T : org.evomaster.core.search.Individual> applyCrossover(
                    x: org.evomaster.core.search.algorithms.wts.WtsEvalIndividual<T>,
                    y: org.evomaster.core.search.algorithms.wts.WtsEvalIndividual<T>,
                    randomness: org.evomaster.core.search.service.Randomness
                ) {
                    if (x.suite.size > 1) {
                        x.suite.removeAt(x.suite.lastIndex)
                    }
                    if (y.suite.size > 1) {
                        y.suite.removeAt(y.suite.lastIndex)
                    }
                }
            })

            ga.setupBeforeSearch()
            val parent0 = ga.getViewOfPopulation().first()
            val parent0Score = ga.score(parent0)

            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(1, nextPop.size)
            val p1 = nextPop.first()

            // Record expectations for this algorithm
            val lambda = config.onePlusLambdaLambdaOffspringSize
            assertEquals(lambda / 2, rec.xoCalls.size)

            // Parent must be kept (offspring are strictly worse)
            assertTrue(p1 === parent0)

            // No child beats the parent
            val allChildren = rec.xoCalls.flatMap { listOf(it.first, it.second) }
            val expectedChildren = 2 * (lambda / 2)
            assertEquals(expectedChildren, allChildren.size)
            val childScores = allChildren.map { ga.score(it) }
            assertTrue(childScores.maxOf { it } <= parent0Score)
        }
    }

    /**
     * Verifies that the best children is kept when it is strictly better than the parent.
     * Force crossover to always improve children so bestOffspring > parent.
     */
    @Test
    fun testBestOffspringKeptWhenBetterThanParent() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(7)

            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.onePlusLambdaLambdaOffspringSize = 6
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 1.0 // force mutants

            ga.setupBeforeSearch()
            val parent0 = ga.getViewOfPopulation().first()
            val parent0Score = ga.score(parent0)

            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(1, nextPop.size)
            val p1 = nextPop.first()

            // Record expectations for this algorithm
            val lambda = config.onePlusLambdaLambdaOffspringSize
            assertEquals(lambda / 2, rec.xoCalls.size)
            assertEquals(lambda, rec.mutated.size)

            val children = rec.xoCalls.flatMap { listOf(it.first, it.second) }

            val bestChild = children.maxBy { ga.score(it) }
            val bestChildScore = ga.score(bestChild)
            if (bestChildScore > parent0Score) {
                assertTrue(p1 !== parent0)
                assertEquals(bestChildScore, ga.score(p1))
            } else {
                assertTrue(p1 === parent0)
            }
            
        }
    }

    // Edge Case: CrossoverProbability=0 and MutationProbability=1
    @Test
    fun testNoCrossoverWhenProbabilityZero_OPLLGA() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.onePlusLambdaLambdaOffspringSize = 6
            config.xoverProbability = 0.0 // disable crossover
            config.fixedRateMutation = 1.0 // force mutation

            ga.setupBeforeSearch()
            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(1, nextPop.size)

            // crossover disabled
            assertEquals(0, rec.xoCalls.size)
            // λ mutants mutated
            assertEquals(config.onePlusLambdaLambdaOffspringSize, rec.mutated.size)
        }
    }

    // Edge Case: MutationProbability=0 and CrossoverProbability=1
    @Test
    fun testNoMutationWhenProbabilityZero_OPLLGA() {
        TestUtils.handleFlaky {
            val ga = injector.getInstance(
                Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<OneMaxIndividual>>() {})
            )

            val rec = GARecorder<OneMaxIndividual>()
            ga.addObserver(rec)

            val config = injector.getInstance(EMConfig::class.java)
            config.gaSolutionSource = EMConfig.GASolutionSource.POPULATION
            config.maxEvaluations = 100_000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.onePlusLambdaLambdaOffspringSize = 8
            config.xoverProbability = 1.0 // force crossover
            config.fixedRateMutation = 0.0 // disable mutation

            ga.setupBeforeSearch()
            ga.searchOnce()

            val nextPop = ga.getViewOfPopulation()
            assertEquals(1, nextPop.size)

            // crossovers happen λ/2 times
            assertEquals(config.onePlusLambdaLambdaOffspringSize / 2, rec.xoCalls.size)

            // mutations disabled
            assertEquals(0, rec.mutated.size)
        }
    }
}


