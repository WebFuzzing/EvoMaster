package org.evomaster.core.problem.rest.aiclassification.defaultexample

import bar.examples.it.spring.aiclassification.defaultexample.DefaultExampleController
import bar.examples.it.spring.body.BodyController
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.EMConfig.EncoderType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class DefaultExampleTest : IntegrationTestRestBase(){


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DefaultExampleController())
        }
    }

    @Test
    fun testEncoding() {

        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/DefaultExample4Testing", queryParams = mapOf(
            "x" to "11223344",
            "y" to "42"
        ))!!

        val x = get.parameters.find { it.name == "x" } as QueryParam
        assertNotNull(x.getGeneForQuery().getWrappedGene(ChoiceGene::class.java)) // examples

        val y = get.parameters.find { it.name == "y" } as QueryParam
        assertNotNull(y.getGeneForQuery().getWrappedGene(ChoiceGene::class.java)) // default

        val z = get.parameters.find { it.name == "z" } as QueryParam
        assertNull(z.getGeneForQuery().getWrappedGene(ChoiceGene::class.java)) // nothing


        val sentinel = -1.0
        val neutral = -2.0

        val wrapper = InputEncoderUtilWrapper(get, EncoderType.NORMAL)
        val encoding = wrapper.encode(sentinel, neutral)
        assertNotNull(encoding)
        assertEquals(3, encoding.size)
        for (i in encoding.indices) {
            assertNotEquals(sentinel, encoding[i])
            assertNotEquals(neutral, encoding[i])
        }
    }
}