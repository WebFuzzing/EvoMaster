package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.ChoiceGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChoiceGeneTest {

    @Test
    fun testOneElement() {
        ChoiceGene("choice",listOf(
                IntegerGene("a",0,10)
        ))
    }

    @Test
    fun testInvalidChildren() {
        assertThrows<IllegalStateException> {
            ChoiceGene("choice",listOf())
        }
    }

    @Test
    fun testInvalidNegativeActiveChoice() {
        assertThrows<IllegalArgumentException> {
            ChoiceGene("choice",listOf(IntegerGene("a",0,10)), activeChoice = -1)
        }
    }

    @Test
    fun testInvalidActiveChoice() {
        assertThrows<IllegalArgumentException> {
            ChoiceGene("choice",listOf(IntegerGene("a",0,10)), activeChoice = 1)
        }
    }

    val rand = Randomness().apply { updateSeed(42) }

    @Test
    fun testRadomize() {
        val gene = ChoiceGene("choice",listOf(
                IntegerGene("a",0,10)
        ))
        gene.doInitialize(rand)
        (gene.getViewOfChildren()[0] as IntegerGene).value = 0
        assertEquals("0", gene.getValueAsPrintableString())
    }

    @Test
    fun testRadomizeTwoChoices() {
        val gene = ChoiceGene("choice",listOf(
                IntegerGene("a",0,10),
                StringGene("b","foo")
        ))
        gene.doInitialize(rand)
        repeat (100) {
            gene.randomize(rand,true)
            (gene.getViewOfChildren()[0] as IntegerGene).value = 1
            (gene.getViewOfChildren()[1] as StringGene).value = "bar"
            val value =  gene.getValueAsPrintableString()
            assertTrue(0 <= gene.activeGeneIndex && gene.activeGeneIndex <=1)
            if (gene.activeGeneIndex == 0) {
                assertEquals("1", value)
            } else {
                assertEquals("\"bar\"", value)
            }
        }

    }

}