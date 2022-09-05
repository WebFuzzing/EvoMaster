package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlBitStringGeneTest {

    @Test
    fun testPrintEmptyBitString() {
        val bitStringGene = SqlBitStringGene("bitstring")
        assertEquals("B${SINGLE_APOSTROPHE_PLACEHOLDER}${SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBitStringAllFalse() {
        val bitStringGene = SqlBitStringGene("bitstring")
        val arrayGene = bitStringGene.getViewOfChildren()[0] as ArrayGene<BooleanGene>
        arrayGene.addElement(BooleanGene("gene0",value = false))
        arrayGene.addElement(BooleanGene("gene1", value = false))
        assertEquals("B${SINGLE_APOSTROPHE_PLACEHOLDER}00${SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

    @Test
    fun testPrintNonEmptyBitString() {
        val bitStringGene = SqlBitStringGene("bitstring")
        val arrayGene = bitStringGene.getViewOfChildren()[0] as ArrayGene<BooleanGene>
        arrayGene.addElement(BooleanGene("gene0", value=false))
        arrayGene.addElement(BooleanGene("gene1", value =true))
        assertEquals("B${SINGLE_APOSTROPHE_PLACEHOLDER}01${SINGLE_APOSTROPHE_PLACEHOLDER}", bitStringGene.getValueAsPrintableString())
    }

}