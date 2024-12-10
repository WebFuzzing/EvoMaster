package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlCompositeGeneTest {

    @Test
    fun testOneIntFieldRow() {
        val compositeGene = SqlCompositeGene(name = "composite", listOf(IntegerGene("number")))
        assertEquals("ROW(0)", compositeGene.getValueAsPrintableString())
    }

    @Test
    fun testTwoIntFieldsRow() {
        val compositeGene = SqlCompositeGene(name = "composite",
                listOf(
                        IntegerGene("left", value = 0),
                        IntegerGene("right", value = 1)
                ))
        assertEquals("ROW(0, 1)", compositeGene.getValueAsPrintableString())
    }

    @Test
    fun testNestedIntFieldsRow() {
        val compositeGene = SqlCompositeGene(name = "outer",
                listOf(
                        IntegerGene("outer", value = 0),
                        SqlCompositeGene(name = "inner",
                                listOf(IntegerGene("number", value = 1), IntegerGene("number", value = 2)))))
        assertEquals("ROW(0, ROW(1, 2))", compositeGene.getValueAsPrintableString())
    }

    @Test
    fun testTwoStringFieldsRow() {
        val compositeGene = SqlCompositeGene(name = "composite",
                listOf(
                        StringGene("left", value = "foo"),
                        StringGene("right", value = "bar")
                ))
        assertEquals("ROW(${SINGLE_APOSTROPHE_PLACEHOLDER}foo${SINGLE_APOSTROPHE_PLACEHOLDER}, ${SINGLE_APOSTROPHE_PLACEHOLDER}bar${SINGLE_APOSTROPHE_PLACEHOLDER})", compositeGene.getValueAsPrintableString())
    }
}