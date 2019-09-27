package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.CharacterClassEscapeRxGene
import org.evomaster.core.search.gene.regex.PatternCharacterBlock
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

open class GeneRegexEcma262VisitorTest : RegexTestTemplate(){

    override fun createGene(regex: String): RegexGene {
        return RegexHandler.createGeneForEcma262(regex)
    }

    @Test
    fun testEmpty(){
        checkSameAsJava("")
    }

    @Test
    fun testBaseStringSingleChar(){

        val regex = "a"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testBaseStringMultiChar(){

        val regex = "abc"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testSingleDigit(){

        val regex = "1"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testMultiDigits(){

        val regex = "123"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testLetterDigits(){

        val regex = "abc123"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }


    @Test
    fun testIssueWithB(){

        val regex = "B123"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testUpperCaseString(){

        val regex = "ABCD"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }


    @Test
    fun testDigitEscape(){

        val regex = "\\d"
        val gene = checkSameAsJava(regex)

        assertTrue(gene.flatView().any { it is CharacterClassEscapeRxGene })
    }

    @Test
    fun testYearPattern(){

        val regex = "\\d\\d\\d\\d-\\d\\d-\\d\\d"
        checkSameAsJava(regex)
    }


    @Test
    fun testQuantifierSingle(){
        checkSameAsJava("a{2}")
    }


    @Test
    fun testQuantifierRange(){
        checkSameAsJava("a{3,5}")
    }

    @Test
    fun testQuantifierOnlyMin(){

        val regex = "^a{2,}$"
        val gene = checkSameAsJava(regex)

        val s = gene.getValueAsRawString()
        //even if unbound, not going to create billion-long strings
        assertTrue(s.length < 5)
    }

    @Test
    fun testQuantifierStar(){
        checkSameAsJava("a*")
    }

    @Test
    fun testQuantifierPlus(){
        checkSameAsJava("a+")
    }

    @Test
    fun testQuantifierOptional(){
        checkSameAsJava("a?")
    }

    @Test
    fun testQuantifierCombined(){
        checkSameAsJava("a*b+c{1}d{2,}e{2,100}")
    }

    @Test
    fun testYearWithQuantifier(){

        val regex = "\\d{4}-\\d{1,2}-\\d{1,2}"
        checkSameAsJava(regex)
    }


    @Test
    fun testAnyChar(){
        checkSameAsJava(".")
    }

    @Test
    fun testAnyCharMulti(){
        checkSameAsJava("...")
    }

    @Test
    fun testAnyCharMixed(){
        checkSameAsJava(".a.b.c.")
    }

    @Test
    fun testParentheses(){
        checkSameAsJava("()")
    }

    @Test
    fun testParenthesesWithText(){
        checkSameAsJava("(hello)")
    }

    @Test
    fun testParenthesesSequence(){
        checkSameAsJava("(a)(b)(c)")
    }

    @Test
    fun testParenthesesNested(){
        checkSameAsJava("(a(bc)(d))")
    }

    @Test
    fun testParenthesesWithQuantifiers(){
        checkSameAsJava("(a1)*(bc)+(d2)?")
    }

    @Test
    fun testDisjunction(){
        checkSameAsJava("a|b")
    }

    @Test
    fun testDisjunctionSequence(){
        checkSameAsJava("a|b|c|def|gh")
    }

    @Test
    fun testDisjunctionNested(){
        checkSameAsJava("(a(b|c))d")
    }

    @Test
    fun testClassRangeSingleChar(){
        checkSameAsJava("[a]")
    }

    @Test
    fun testClassRangeMultiChars(){
        checkSameAsJava("[abc]")
    }

    @Test
    fun testClassRangeMultiCharsWithSpecialSymbols(){
        checkSameAsJava("[abc123(){}/?+*]")
    }

    @Test
    fun testClassRangeChars(){
        checkSameAsJava("[a-z]")
    }

    @Test
    fun testClassRangeDigits(){
        checkSameAsJava("[0-9]")
    }

    @Test
    fun testClassRangeMulti(){
        checkSameAsJava("[a-zA-Z0-9]")
    }

    @Test
    fun testClassRangeQuantifier(){
        checkSameAsJava("[0-9]{2}")
    }

    @Test
    fun testClassRangeIndExample(){
        checkSameAsJava("/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?")
    }

    @Test
    fun testAssertionStart(){
        checkSameAsJava("^a")
    }

    @Test
    fun testAssertionEnd(){
        checkSameAsJava("a$")
    }

    @Test
    fun testAssertionStartAndEnd(){
        checkSameAsJava("^a$")
    }

    @Test
    fun testAssertionSequence(){
        checkSameAsJava("^a|b$|^c$|ef(gh)|^i(l)|(m)n$")
    }


    @Test
    fun testAssertionsInParentheses(){
        checkSameAsJava("^((.*))$")
    }

    @Test
    fun testAssertionsInDisjunctions(){
        checkSameAsJava("^(bar)\$|^(foo)\$|^hello|world\$")
    }

    @Test
    fun testIssueWithE(){
        checkSameAsJava("(a|A)(b|B)(c|C)123(d|D)(e|E)(f|F)")
    }

    @Test
    fun testCanSampleInSimpleDisjunction(){
        checkCanSample("a|b", listOf("a", "b"), 500)
    }

    @Test
    fun testCanSampleIn2Disjunction(){
        checkCanSample("a|b|c", listOf("a", "b", "c"), 500)
    }

    @Test
    fun testCanSamplePair(){
        checkCanSample("(a|b)(c|d)", listOf("ac", "ad", "bc", "bd"), 500)
    }

    @Test
    fun testCanSamplePairIgnoreCase(){
        checkCanSample("(a|A)(b|B)", listOf("ab", "aB", "Ab", "AB"), 500)
    }

    @Test
    fun testCanSampleEQPairIgnoreCase(){
        checkCanSample("(e|E)(q|Q)", listOf("eq", "eQ", "Eq", "EQ"), 500)
    }

    @Test
    fun testCanSampleWhenPrefix(){
        checkCanSample("x(a|b|c)", listOf("xa", "xb", "xc"), 500)
    }

    @Test
    fun testCanSampleSequence(){
        checkCanSample("x(a|b)123(c|d)y", listOf("xa123cy","xb123cy","xa123dy","xb123dy"), 500)
    }

    @Test
    fun testCanSampleSequenceInFullMatch(){
        checkCanSample("^(x(a|b)123(c|d)y)$", listOf("xa123cy","xb123cy","xa123dy","xb123dy"), 500)
    }


    @Test
    fun testIssueWithNestedParentheses(){
        // p = 1 / 2^6 = 1 / 64
        checkCanSample("^((a|A)(b|B)(c|C)123(e|E)(f|F)(d|D))$", "aBc123EFd", 10_000)
    }
}