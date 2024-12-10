package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SqlTextSearchQueryGeneTest {

    @Test
    fun testEmptyTextSearchQuery() {
        val gene = SqlTextSearchQueryGene("textSearchVector")
        Assertions.assertEquals("to_tsquery(${SINGLE_APOSTROPHE_PLACEHOLDER}${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }

    @Test
    fun testOneElementTextSearchQuery() {
        val gene = SqlTextSearchQueryGene("textSearchVector")

        val textLexemes = gene.getViewOfChildren()[0] as ArrayGene<StringGene>
        val stringGene = textLexemes.template.copy() as StringGene
        stringGene.value = "foo"
        textLexemes.addElement(stringGene)
        Assertions.assertEquals("to_tsquery(${SINGLE_APOSTROPHE_PLACEHOLDER}foo${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }
    @Test
    fun testManyElementTextSearchQuery() {
        val gene = SqlTextSearchQueryGene("textSearchVector")

        val textLexemes = gene.getViewOfChildren()[0] as ArrayGene<StringGene>
        val stringGene0 = textLexemes.template.copy() as StringGene
        stringGene0.value = "foo"

        val stringGene1 = textLexemes.template.copy() as StringGene
        stringGene1.value = "bar"

        val stringGene2 = textLexemes.template.copy() as StringGene
        stringGene2.value = "cat"

        textLexemes.addElement(stringGene0)
        textLexemes.addElement(stringGene1)
        textLexemes.addElement(stringGene2)

        Assertions.assertEquals("to_tsquery(${SINGLE_APOSTROPHE_PLACEHOLDER}foo & bar & cat${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }


}