package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.RegexGene
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 11-Sep-19.
 */
class GeneRegexJavaVisitorTest : GeneRegexEcma262VisitorTest() {

    override fun createGene(regex: String): RegexGene {
        return RegexHandler.createGeneForJVM(regex)
    }

    @Test
    fun testQuote(){
        checkSameAsJava("\\Qfoo.com\\E")
    }

    @Test
    fun testEmptyQuote(){
        checkSameAsJava("\\Q\\E")
    }

    @Test
    fun testBlankQuote(){
        checkSameAsJava("\\Q    \\E")
    }


    @Test
    fun testQuoteWholeLine(){
        checkSameAsJava("^(\\Qfoo.com\\E)$")
    }

    @Test
    fun testAssertionsAndQuotes(){
        checkSameAsJava("^((.*))$|^(\\Qfoo.com\\E)$")
    }

    @Test
    fun testQuotesInTheMiddle(){
        checkSameAsJava("^(\\d{4}-\\d{1,2}-\\d{1,2})\\Q-\\E(\\d+)$")
    }

    @Test
    fun testIssueQuotedE(){
        checkSameAsJava("\\Qfooebar\\E")
        checkSameAsJava("\\QfooEbar\\E")
    }

    @Test
    fun testIssueWithControlCharactersInIgnoreCase(){
        val s = "a[](){}\\\"^$.b"
        checkCanSample(RegexUtils.ignoreCaseRegex(s), listOf(s.toUpperCase(), s.toLowerCase()), 200)
    }
}