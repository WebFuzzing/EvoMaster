package org.evomaster.core.search.algorithms

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.ExecutionPhaseController
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_TOLERANCE = 1e-9

class CroAlgorithmTest {

    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
            .build().createInjector()
    }

    // Verifies that CRO can find the optimal solution for the OneMax problem
    @Test
    fun testCroAlgorithmFindsOptimum() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            injector.getInstance(Randomness::class.java).updateSeed(42)

            // Keep defaults close to other GA tests
            config.maxEvaluations = 10000
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()
            val solution = cro.search()
            epc.finishSearch()

            assertTrue(solution.individuals.size == 1)
            assertEquals(OneMaxSampler.DEFAULT_N.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }

    @Test
    fun testUniMolecular_DecompositionPath_usesDecomposition() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(123L)

            // Single molecule guarantees uni-molecular branch regardless of collision rate
            config.populationSize = 1
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            // Force decomposition check to true without touching internal state (0 > -1)
            config.croDecompositionThreshold = -1

            // Initialize algorithm (creates molecules and initialEnergy)
            cro.setupBeforeSearch()

            // Prepare deterministic potentials and disable mutations/crossover side effects
            cro.usePotentialSequence(listOf(10.0, 3.0, 5.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            // Run single step
            cro.searchOnce()

            // Expect parent replaced by two offspring
            val moleculesAfter = cro.getMoleculesSnapshot()
            assertEquals(2, moleculesAfter.size)
            // Offspring collisions reset to 0 and KE non-negative
            assertTrue(moleculesAfter[0].numCollisions == 0 && moleculesAfter[0].kineticEnergy >= 0.0)
            assertTrue(moleculesAfter[1].numCollisions == 0 && moleculesAfter[1].kineticEnergy >= 0.0)
        }
    }

    

    @Test
    fun testUniMolecular_DecompositionPath_rejected_NoReplacementAndParentCollisionsIncrement() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(789L)

            // Uni-molecular branch
            config.populationSize = 1
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            // Force decomposition check true
            config.croDecompositionThreshold = -1

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()[0]

            // Potentials: parent=10, children=8 and 9 -> net = 10 - (8+9) = -7 (negative)
            // Container starts at 0, borrow fails, decomposition returns null
            cro.usePotentialSequence(listOf(10.0, 8.0, 9.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val afterList = cro.getMoleculesSnapshot()
            // No replacement (still one molecule)
            assertEquals(1, afterList.size)
            val after = afterList[0]
            // Parent collisions incremented by +1 on failed decomposition
            assertEquals(before.numCollisions + 1, after.numCollisions)
            // KE unchanged (decomposition did not apply)
            assertEquals(before.kineticEnergy, after.kineticEnergy, TEST_TOLERANCE)
        }
    }

    @Test
    fun testUniMolecular_OnWallPath_usesOnWall() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(456L)

            config.populationSize = 1
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0

            cro.setupBeforeSearch()

            // Make decompositionCheck false by setting very high threshold
            config.croDecompositionThreshold = Int.MAX_VALUE
            val before = cro.getMoleculesSnapshot()[0]

            // Deterministic on-wall: old=10, new=7 => net=3, accept and update KE/container
            cro.usePotentialSequence(listOf(10.0, 7.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            // Still one molecule, but updated (collisions incremented and KE >= 0)
            val moleculesAfter = cro.getMoleculesSnapshot()
            assertEquals(1, moleculesAfter.size)
            val after = moleculesAfter[0]
            assertTrue(after.numCollisions == before.numCollisions + 1)
            assertTrue(after.kineticEnergy >= 0.0)
        }
    }

    @Test
    fun testUniMolecular_OnWallPath_negativeNet_NoChange() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(321L)

            config.populationSize = 1
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0

            // Force on-wall branch (decomposition false)
            config.croDecompositionThreshold = Int.MAX_VALUE

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()[0]

            // Potentials: old=10, new=15 -> net = -5 (reject), so no change
            cro.usePotentialSequence(listOf(10.0, 15.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val afterList = cro.getMoleculesSnapshot()
            assertEquals(1, afterList.size)
            val after = afterList[0]
            // On-wall rejected: collisions and KE unchanged
            assertEquals(before.numCollisions, after.numCollisions)
            assertEquals(before.kineticEnergy, after.kineticEnergy, TEST_TOLERANCE)
        }
    }

    @Test
    fun testInterMolecular_SynthesisPath_accepted_FusesAndSizeDecreases() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(111L)

            // Force inter-molecular branch and synthesis check true (KE=0 <= threshold)
            config.populationSize = 2
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            config.croMolecularCollisionRate = 1.0
            config.croSynthesisThreshold = 0.0

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()
            assertEquals(2, before.size)

            // Potentials: first=10, second=9, fused=15 -> net=19-15=4 >= 0 => accept
            cro.usePotentialSequence(listOf(10.0, 9.0, 15.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val after = cro.getMoleculesSnapshot()
            assertEquals(1, after.size) // fused replaced the pair
            assertTrue(after[0].kineticEnergy >= 0.0)
            assertEquals(0, after[0].numCollisions)
        }
    }

    @Test
    fun testInterMolecular_SynthesisPath_rejected_ParentsCollisionsIncrementAndNoSizeChange() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(112L)

            config.populationSize = 2
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            config.croMolecularCollisionRate = 1.0
            config.croSynthesisThreshold = 0.0 // synthesisCheck true

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()
            assertEquals(2, before.size)
            val beforeA = before[0]
            val beforeB = before[1]

            // Potentials: first=10, second=9, fused=25 -> net=19-25=-6 => reject, parents collisions +1
            cro.usePotentialSequence(listOf(10.0, 9.0, 25.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val after = cro.getMoleculesSnapshot()
            assertEquals(2, after.size) // size unchanged
            // Both parents collisions incremented by 1 on failed synthesis
            assertEquals(beforeA.numCollisions + 1, after[0].numCollisions)
            assertEquals(beforeB.numCollisions + 1, after[1].numCollisions)
        }
    }

    @Test
    fun testInterMolecular_IneffectiveCollision_accepted_ReplacesBothAndCollisionsIncrement() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<TestCroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(113L)

            config.populationSize = 2
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            config.croMolecularCollisionRate = 1.0
            config.croSynthesisThreshold = -1.0 // synthesisCheck false

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()

            // Potentials:
            // first=10, second=9, updatedFirst=12, updatedSecond=5 =>
            // net = (10+9) - (12+5) = 2 >= 0 -> accept
            cro.usePotentialSequence(listOf(10.0, 9.0, 12.0, 5.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val after = cro.getMoleculesSnapshot()
            assertEquals(2, after.size)
            // Collisions incremented for both
            assertEquals(before[0].numCollisions + 1, after[0].numCollisions)
            assertEquals(before[1].numCollisions + 1, after[1].numCollisions)
            // KE non-negative for both
            assertTrue(after[0].kineticEnergy >= 0.0)
            assertTrue(after[1].kineticEnergy >= 0.0)
        }
    }

    @Test
    fun testInterMolecular_IneffectiveCollision_rejected_NoChange() {
        TestUtils.handleFlaky {
            val cro = injector.getInstance(
                Key.get(object : TypeLiteral<CroAlgorithm<OneMaxIndividual>>() {})
            )

            val config = injector.getInstance(EMConfig::class.java)
            val randomness = injector.getInstance(Randomness::class.java)
            randomness.updateSeed(114L)

            config.populationSize = 2
            config.algorithm = EMConfig.Algorithm.CRO
            config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            config.maxEvaluations = 1
            config.croInitialKineticEnergy = 0.0
            config.croMolecularCollisionRate = 1.0
            config.croSynthesisThreshold = -1.0 // synthesisCheck false

            cro.setupBeforeSearch()
            val before = cro.getMoleculesSnapshot()

            // Potentials:
            // first=10, second=9, updatedFirst=20, updatedSecond=10 =>
            // net = (19) - (30) = -11 -> reject
            cro.usePotentialSequence(listOf(10.0, 9.0, 20.0, 10.0))
            cro.useMutationHook { }
            cro.useCrossoverHook { _, _ -> }

            cro.searchOnce()

            val after = cro.getMoleculesSnapshot()
            assertEquals(2, after.size)
            // No change when rejected
            assertEquals(before[0].numCollisions, after[0].numCollisions)
            assertEquals(before[1].numCollisions, after[1].numCollisions)
            assertEquals(before[0].kineticEnergy, after[0].kineticEnergy, TEST_TOLERANCE)
            assertEquals(before[1].kineticEnergy, after[1].kineticEnergy, TEST_TOLERANCE)
        }
    }
}

private class TestCroAlgorithm<T> : CroAlgorithm<T>() where T : Individual {

    private var potentialOverrides: ArrayDeque<Double>? = null
    private var mutateOverride: ((WtsEvalIndividual<T>) -> Unit)? = null
    private var crossoverOverride: ((WtsEvalIndividual<T>, WtsEvalIndividual<T>) -> Unit)? = null

    fun usePotentialSequence(values: List<Double>) {
        potentialOverrides = ArrayDeque(values)
    }

    fun usePotentialSequence(vararg values: Double) = usePotentialSequence(values.toList())

    fun useMutationHook(hook: ((WtsEvalIndividual<T>) -> Unit)?) {
        mutateOverride = hook
    }

    fun useCrossoverHook(hook: ((WtsEvalIndividual<T>, WtsEvalIndividual<T>) -> Unit)?) {
        crossoverOverride = hook
    }

    override fun computePotential(evaluatedSuite: WtsEvalIndividual<T>): Double {
        val overrides = potentialOverrides
        return if (overrides != null && overrides.isNotEmpty()) {
            overrides.removeFirst()
        } else {
            super.computePotential(evaluatedSuite)
        }
    }

    override fun applyMutation(wts: WtsEvalIndividual<T>) {
        val hook = mutateOverride
        if (hook != null) {
            hook(wts)
        } else {
            super.applyMutation(wts)
        }
    }

    override fun applyCrossover(first: WtsEvalIndividual<T>, second: WtsEvalIndividual<T>) {
        val hook = crossoverOverride
        if (hook != null) {
            hook(first, second)
        } else {
            super.applyCrossover(first, second)
        }
    }
}


