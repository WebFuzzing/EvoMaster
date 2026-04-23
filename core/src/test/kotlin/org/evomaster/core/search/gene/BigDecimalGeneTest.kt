package org.evomaster.core.search.gene

import com.google.inject.Injector
import com.google.inject.Module
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun testSetValueWithLong() {
        val minValue = 0L
        val midValue = 1005L
        val maxValue = 1006L

        val gene = BigDecimalGene("gene",
            min = BigDecimal(minValue),
            max = BigDecimal(maxValue),
            precision = 3)

        val rand = Randomness()
        rand.updateSeed(42)
        gene.doInitialize(rand)
        gene.setValueWithLong(midValue)

        // As precision is 3, 1005L is rounded up to 1010L.
        // Therefore, the comparison 1010L < 1006L fails.
        // After fixing BigDecimalGene, it is clamped to 1006L.
        assertTrue(gene.value <= gene.getMaximum())
    }


    @Test
    fun testSetValueWithLongOutsideRange() {
        val minValue = 0L
        val maxValue = 10L

        val gene = BigDecimalGene("gene",
            min = BigDecimal(minValue),
            max = BigDecimal(maxValue))

        val rand = Randomness()
        rand.updateSeed(42)
        gene.doInitialize(rand)

        /*
         * This should not throw an exception. There
         * might be scenarios where the client wants to
         * set a value outside the range.
         */
        gene.setValueWithLong(1000L)

        assertEquals(BigDecimal(1000L) , gene.value)

    }

}
