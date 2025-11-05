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
    
}


