package org.evomaster.core.search

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.constant.ConstantIndividual
import org.evomaster.core.search.constant.ConstantModule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class MioAlgorithmOnConstantTest {

    val injector: Injector = LifecycleInjector.builder()
            .withModules(* arrayOf<Module>(ConstantModule(), BaseModule()))
            .build().createInjector()

    @Test
    fun testMIO(){

        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<ConstantIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)
        config.maxFitnessEvaluations = 10000
        config.archiveTargetLimit = 1
        config.probOfRandomSampling = 0.0

        val solution = mio.search()

        Assertions.assertEquals(1.0, solution.overall.computeFitnessScore(), 0.001);
        Assertions.assertEquals(1, solution.individuals.size)
    }
}