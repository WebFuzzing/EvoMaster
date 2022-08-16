package org.evomaster.core.search.gene

import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalGeneTest {

    @Test
    fun testMutationsWithinRange() {
        val randomness = Randomness()

        val config = EMConfig()
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        val time = SearchTimeController()

        val apc = AdaptiveParameterControl()

        val configField = AdaptiveParameterControl::class.java.getDeclaredField("config")
        configField.isAccessible = true
        configField.set(apc, config)
        configField.isAccessible = false

        val timeField = AdaptiveParameterControl::class.java.getDeclaredField("time")
        timeField.isAccessible = true
        timeField.set(apc, time)
        timeField.isAccessible = false

        val configurationField = SearchTimeController::class.java.getDeclaredField("configuration")
        configurationField.isAccessible = true
        configurationField.set(time, config)
        configurationField.isAccessible = false

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