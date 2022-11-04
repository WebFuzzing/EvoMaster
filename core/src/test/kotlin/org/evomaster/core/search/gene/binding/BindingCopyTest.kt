package org.evomaster.core.search.gene.binding

import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BindingCopyTest {

    @Test
    fun testGeneBinding(){
        val geneA = IntegerGene("a", value = 1)
        val geneB = IntegerGene("b", value = 2)
        val geneC = FloatGene("c", value = 3.0f)
        val geneD = DoubleGene("c", value = 4.0)
        val geneE = LongGene("d", value= 5L)
        val geneF = StringGene("f", value = "6")

        // integer <-> integer, Long, float, double, string
        geneA.addBindingGene(geneB)
        geneA.addBindingGene(geneC)
        geneA.addBindingGene(geneD)
        geneA.addBindingGene(geneE)
        geneA.addBindingGene(geneF)

        geneA.syncBindingGenesBasedOnThis()
        assertEquals(1, geneB.value)
        assertEquals(1f, geneC.value)
        assertEquals(1.0, geneD.value)
        assertEquals(1L, geneE.value)
        assertEquals("1", geneF.value)
    }

    @Test
    fun testBindingCycle(){
        val geneA = IntegerGene("a", value = 1)
        val geneB = IntegerGene("b", value = 2)
        val geneC = FloatGene("c", value = 3.0f)

        geneA.addBindingGene(geneB)
        geneB.addBindingGene(geneA)

        geneB.addBindingGene(geneC)
        geneC.addBindingGene(geneB)

        geneA.syncBindingGenesBasedOnThis()
        assertEquals(1, geneB.value)
        assertEquals(1f, geneC.value)

        geneC.value = 3.0f
        geneC.syncBindingGenesBasedOnThis()
        assertEquals(3, geneA.value)
        assertEquals(3, geneB.value)
    }

    @Test
    fun testCopy(){
        val geneA = IntegerGene("a", value = 1)
        val geneB = IntegerGene("b", value = 2)
        val geneC = FloatGene("c", value = 3.0f)
        val geneD = DoubleGene("c", value = 4.0)
        val geneE = LongGene("d", value= 5L)

        val ind = BindingIndividual(mutableListOf(geneA, geneB, geneC, geneD, geneE))
        geneA.addBindingGene(geneE)
        geneE.addBindingGene(geneA)
        assertTrue(ind.genes.first().isDirectBoundWith(ind.genes.last()))

        val copy = ind.copy() as BindingIndividual
        assertTrue(copy.genes.first().isDirectBoundWith(copy.genes.last()))
    }

}