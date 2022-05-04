package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.BooleanGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlBitStringGeneTest {

    @Test
    fun testPrintEmptyBitString() {
        val bitStringGene = SqlBitStringGene("bitstring")
        assertEquals("B${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBitStringAllFalse() {
        val bitStringGene = SqlBitStringGene("bitstring")
        val arrayGene = bitStringGene.innerGene()[0] as ArrayGene<BooleanGene>
        arrayGene.addElement(BooleanGene("gene0",value = false))
        arrayGene.addElement(BooleanGene("gene1", value = false))
        assertEquals("B${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}00${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBitString() {
        val bitStringGene = SqlBitStringGene("bitstring")
        val arrayGene = bitStringGene.innerGene()[0] as ArrayGene<BooleanGene>
        arrayGene.addElement(BooleanGene("gene0", value=false))
        arrayGene.addElement(BooleanGene("gene1", value =true))
        assertEquals("B${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}01${SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

}