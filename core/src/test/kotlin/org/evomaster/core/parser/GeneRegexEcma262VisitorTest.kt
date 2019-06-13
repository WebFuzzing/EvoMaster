package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.CharacterClassEscapeRxGene
import org.evomaster.core.search.gene.regex.PatternCharacterBlock
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class GeneRegexEcma262VisitorTest{

    private fun checkRegex(regex: String) : RegexGene{

        val randomness = Randomness()

        val gene = RegexHandler.createGeneForEcma262(regex)

        for(seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false, listOf())

            val instance = gene.getValueAsRawString()

            /*
                Ecma262 and Java regex are not exactly the same.
                But for the base types we test in this class, they
                should be equivalent.
            */

            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(instance)
            assertTrue(matcher.find(), "String not matching regex:\n$regex\n$instance")
        }

        return gene
    }

    @Test
    fun testEmpty(){
        checkRegex("")
    }

    @Test
    fun testBaseStringSingleChar(){

        val regex = "a"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testBaseStringMultiChar(){

        val regex = "abc"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testSingleDigit(){

        val regex = "1"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testMultiDigits(){

        val regex = "123"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testLetterDigits(){

        val regex = "abc123"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }


    @Test
    fun testIssueWithB(){

        val regex = "B123"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }

    @Test
    fun testUpperCaseString(){

        val regex = "ABCD"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is PatternCharacterBlock })
    }


    @Test
    fun testDigitEscape(){

        val regex = "\\d"
        val gene = checkRegex(regex)

        assertTrue(gene.flatView().any { it is CharacterClassEscapeRxGene })
    }

    @Test
    fun testYearPattern(){

        val regex = "\\d\\d\\d\\d-\\d\\d-\\d\\d"
        checkRegex(regex)
    }


    @Test
    fun testQuantifierSingle(){
        checkRegex("a{2}")
    }


    @Test
    fun testQuantifierRange(){
        checkRegex("a{3,5}")
    }

    @Test
    fun testQuantifierOnlyMin(){

        val regex = "^a{2,}$"
        val gene = checkRegex(regex)

        val s = gene.getValueAsRawString()
        //even if unbound, not going to create billion-long strings
        assertTrue(s.length < 5)
    }

    @Test
    fun testQuantifierStar(){
        checkRegex("a*")
    }

    @Test
    fun testQuantifierPlus(){
        checkRegex("a+")
    }

    @Test
    fun testQuantifierOptional(){
        checkRegex("a?")
    }

    @Test
    fun testQuantifierCombined(){
        checkRegex("a*b+c{1}d{2,}e{2,100}")
    }

    @Test
    fun testYearWithQuantifier(){

        val regex = "\\d{4}-\\d{1,2}-\\d{1,2}"
        checkRegex(regex)
    }


    @Test
    fun testAnyChar(){
        checkRegex(".")
    }

    @Test
    fun testAnyCharMulti(){
        checkRegex("...")
    }

    @Test
    fun testAnyCharMixed(){
        checkRegex(".a.b.c.")
    }

    @Test
    fun testParentheses(){
        checkRegex("()")
    }

    @Test
    fun testParenthesesWithText(){
        checkRegex("(hello)")
    }

    @Test
    fun testParenthesesSequence(){
        checkRegex("(a)(b)(c)")
    }

    @Test
    fun testParenthesesNested(){
        checkRegex("(a(bc)(d))")
    }

    @Test
    fun testParenthesesWithQuantifiers(){
        checkRegex("(a1)*(bc)+(d2)?")
    }

    @Test
    fun testDisjunction(){
        checkRegex("a|b")
    }

    @Test
    fun testDisjunctionSequence(){
        checkRegex("a|b|c|def|gh")
    }

    @Test
    fun testDisjunctionNested(){
        checkRegex("(a(b|c))d")
    }

    @Test
    fun testClassRangeSingleChar(){
        checkRegex("[a]")
    }

    @Test
    fun testClassRangeMultiChars(){
        checkRegex("[abc]")
    }

    @Test
    fun testClassRangeMultiCharsWithSpecialSymbols(){
        checkRegex("[abc123(){}/?+*]")
    }

    @Test
    fun testClassRangeChars(){
        checkRegex("[a-z]")
    }

    @Test
    fun testClassRangeDigits(){
        checkRegex("[0-9]")
    }

    @Test
    fun testClassRangeMulti(){
        checkRegex("[a-zA-Z0-9]")
    }

    @Test
    fun testClassRangeQuantifier(){
        checkRegex("[0-9]{2}")
    }

    @Test
    fun testClassRangeIndExample(){
        checkRegex("/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?")
    }

    @Test
    fun testAssertionStart(){
        checkRegex("^a")
    }

    @Test
    fun testAssertionEnd(){
        checkRegex("a$")
    }

    @Test
    fun testAssertionStartAndEnd(){
        checkRegex("^a$")
    }

    @Test
    fun testAssertionSequence(){
        checkRegex("^a|b$|^c$|ef(gh)|^i(l)|(m)n$")
    }


}