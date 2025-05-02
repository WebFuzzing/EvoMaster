package org.evomaster.core.problem.rest.param

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class BodyParamTest {

    @Test
    fun testBodyParamPrintableString() {
        val stringGene = StringGene("stringGene")
        val enumGene = EnumGene("contentType", listOf("application/json"))
        stringGene.value = "Hello World"
        enumGene.index = 0
        val bodyParam = BodyParam(gene = stringGene, typeGene = enumGene)

        assertFalse(bodyParam.contentRemoveQuotesGene.gene.value)
        val printableStringWithQuotes = bodyParam.primaryGene()
            .getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.DEFAULT)
        assertEquals("\"Hello World\"", printableStringWithQuotes)

        bodyParam.contentRemoveQuotesGene.gene.value = true
        val printableStringWithoutQuotes =
            bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.DEFAULT)
        assertEquals("Hello World", printableStringWithoutQuotes)
    }


    @Test
    fun testSeeGenes() {
        val stringGene = StringGene("stringGene")
        val enumGene = EnumGene("contentType", listOf("application/json"))
        stringGene.value = "Hello World"
        enumGene.index = 0
        val bodyParam = BodyParam(gene = stringGene, typeGene = enumGene)
        assertEquals(3, bodyParam.seeGenes().size)
    }


    @Test
    fun testEventuallySendUnquoteJsonString() {

        /*
            this test was making wrong assumption.
            once the choice of quoted/unquoted is made at sampling, it should
            never be changed via mutation.
         */

        var caseTrue = false
        var caseFalse = false
        var counter = 0L

        repeat(100) {
            val stringGene = StringGene("stringGene")
            val enumGene = EnumGene("contentType", listOf("application/json"))
            stringGene.value = "Hello World"
            enumGene.index = 0
            val bodyParam = BodyParam(gene = stringGene, typeGene = enumGene)

            val restPath = RestPath("/foo/bar")
            val restCallAction =
                RestCallAction(
                    id = "post",
                    verb = HttpVerb.POST,
                    path = restPath,
                    parameters = mutableListOf(bodyParam)
                )

            assertFalse(bodyParam.contentRemoveQuotesGene.gene.value)
            val randomness = Randomness().apply { updateSeed(counter++) }
            restCallAction.doInitialize(randomness)
            val sendUnquoteJsonStringValue = bodyParam.contentRemoveQuotesGene.gene.value
            if (sendUnquoteJsonStringValue) {
                caseTrue = true
            } else {
                caseFalse = true
            }

            repeat(100) {
                restCallAction.randomize(randomness, forceNewValue = true)
                //must not change once set
                assertEquals(sendUnquoteJsonStringValue, bodyParam.contentRemoveQuotesGene.gene.value)
            }
        }
        // tossing the coin 100 times should show at least one ``true'' value in bodyParam.contentSendUnquoteJsonStringGene
        assertTrue(caseTrue)
        assertTrue(caseFalse)
    }


    @Test
    fun testNeverSendUnquoteJsonString() {
        val bodyGene = ObjectGene(
            "foo",
            fields = listOf(
                LongGene("id", 1L),
                DoubleGene("doubleValue", 2.0),
                IntegerGene("intValue", 3),
                FloatGene("floatValue", 4f)
            )
        )
        val enumGene = EnumGene("contentType", listOf("application/json"))
        enumGene.index = 0
        val bodyParam = BodyParam(gene = bodyGene, typeGene = enumGene)

        val restPath = RestPath("/foo/bar")
        val restCallAction =
            RestCallAction(id = "post", verb = HttpVerb.POST, path = restPath, parameters = mutableListOf(bodyParam))

        assertFalse(bodyParam.contentRemoveQuotesGene.gene.value)
        val randomness = Randomness()
        restCallAction.doInitialize(randomness)
        repeat(1_000_000) {
            restCallAction.randomize(randomness, forceNewValue = false)
            // since the body is not a string, it never sends an unquoted string
            assertFalse(bodyParam.contentRemoveQuotesGene.gene.value)
        }
    }

    @Test
    fun testInitializeAllGenes() {
        val bodyGene = ObjectGene(
            "foo",
            fields = listOf(
                LongGene("id", 1L),
                DoubleGene("doubleValue", 2.0),
                IntegerGene("intValue", 3),
                FloatGene("floatValue", 4f)
            )
        )
        val enumGene = EnumGene("contentType", listOf("application/json"))
        enumGene.index = 0
        val bodyParam = BodyParam(gene = bodyGene, typeGene = enumGene)

        val restPath = RestPath("/foo/bar")
        val restCallAction =
            RestCallAction(id = "post", verb = HttpVerb.POST, path = restPath, parameters = mutableListOf(bodyParam))

        val individual =
            RestIndividual(mutableListOf(restCallAction), SampleType.RANDOM, dbInitialization = mutableListOf())
        assertFalse(individual.isInitialized())

        val randomness = Randomness()
        individual.doInitialize(randomness)
        assertTrue(individual.isInitialized())

    }
}
