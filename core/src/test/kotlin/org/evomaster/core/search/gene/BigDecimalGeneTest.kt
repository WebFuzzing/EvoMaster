package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalGeneTest {

    @Test
    fun testMutationsWithinRange() {

        val injector: Injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(BaseModule(emptyArray(), true)))
                .build().createInjector()

        val randomness = injector.getInstance(Randomness::class.java)
        val config = injector.getInstance(EMConfig::class.java)
        val apc = injector.getInstance(AdaptiveParameterControl::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
        config.focusedSearchActivationTime = 0.5
        config.maxEvaluations = 10
        config.useTimeInFeedbackSampling = false
        config.seed = 42

        val gene = BigDecimalGene("gene",
                min = BigDecimal(-100),
                max = BigDecimal(100),
                floatingPointMode = false)
        gene.value = BigDecimal(0)
        val initialValue = gene.value

        repeat(1000) {
            gene.mutateFloatingPointNumber(randomness,
                    apc = apc)

            assertTrue(gene.isLocallyValid(), "BigDecimalRange with range [${gene.min},${gene.max}] and initial value ${initialValue} lead to ${gene.value} after ${it} mutations")
        }
    }
}