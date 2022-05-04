package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.gene.sql.SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER
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

        val textLexemes = gene.innerGene()[0] as ArrayGene<StringGene>
        textLexemes.addElement(StringGene("lexeme",value="foo bar"))
        Assertions.assertEquals("to_tsquery(${SINGLE_APOSTROPHE_PLACEHOLDER}foo bar${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }
    @Test
    fun testManyElementTextSearchQuery() {
        val gene = SqlTextSearchQueryGene("textSearchVector")

        val textLexemes = gene.innerGene()[0] as ArrayGene<StringGene>
        textLexemes.addElement(StringGene("lexeme",value="foo"))
        textLexemes.addElement(StringGene("lexeme",value="bar"))
        textLexemes.addElement(StringGene("lexeme",value="cat"))

        Assertions.assertEquals("to_tsquery(${SINGLE_APOSTROPHE_PLACEHOLDER}foo & bar & cat${SINGLE_APOSTROPHE_PLACEHOLDER})", gene.getValueAsPrintableString())
    }


}