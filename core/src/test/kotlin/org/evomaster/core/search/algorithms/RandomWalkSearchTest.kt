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
import org.evomaster.core.search.service.SearchTimeController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class RandomWalkSearchTest {



    @Test
    fun testRandomWalkSearch(){
        TestUtils.handleFlaky {

            val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
                .build().createInjector()

            val rw = injector.getInstance(Key.get(
                    object : TypeLiteral<RandomWalkAlgorithm<OneMaxIndividual>>() {}))

            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = EMConfig.Algorithm.RW
            config.maxEvaluations = 3000
            config.stoppingCriterion = EMConfig.StoppingCriterion.INDIVIDUAL_EVALUATIONS

            val sampler = injector.getInstance(OneMaxSampler::class.java)
            val n = 10
            sampler.n = n

            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()

            val solution = rw.search()

            assertEquals(n.toDouble(), solution.overall.computeFitnessScore(), 0.001)
        }
    }
}