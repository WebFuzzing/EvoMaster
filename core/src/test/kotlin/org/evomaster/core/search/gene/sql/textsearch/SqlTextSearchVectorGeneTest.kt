package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SqlTextSearchVectorGeneTest {

    @Test
    fun testEmptyTextSearchVector() {
        val gene = SqlTextSearchVectorGene("textSearchVector")
        (gene.getViewOfChildren()[0] as StringGene).value =""
        Assertions.assertEquals("to_tsvector(${SINGLE_APOSTROPHE_PLACEHOLDER}${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }

    @Test
    fun testTextSearchVector() {
        val gene = SqlTextSearchVectorGene("textSearchVector")
        val textLexemes = gene.getViewOfChildren()[0] as StringGene
        textLexemes.value = "foo bar"
        Assertions.assertEquals("to_tsvector(${SINGLE_APOSTROPHE_PLACEHOLDER}foo bar${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }


}