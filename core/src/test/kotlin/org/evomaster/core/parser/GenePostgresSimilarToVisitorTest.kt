package org.evomaster.core.parser


import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

/**
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresSimilarToVisitorTest {

    private fun checkSimilarTo(similarTo: String) : RegexGene {
        //used when syntax is the same as in Java regex
        return checkSimilarTo(similarTo, similarTo)
    }

    private fun checkSimilarTo(similarTo: String, javaRegex: String) : RegexGene {

        val randomness = Randomness()

        val gene = RegexHandler.createGeneForPostgresSimilarTo(similarTo)

        for(seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false, listOf())

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            assertTrue(matcher.find(), "String not matching SIMILAR TO:\n$similarTo\n$instance")
        }

        return gene
    }


    @Test
    fun testEmpty(){
        checkSimilarTo("", "")
    }

    @Test
    fun testBaseStringSingleChar(){
        checkSimilarTo("a")
    }

    @Test
    fun testBaseStringMultiChar(){
        checkSimilarTo("abc")
    }

    @Test
    fun testSingleDigit(){
        checkSimilarTo("1")
    }

    @Test
    fun testMultiDigits(){
        checkSimilarTo("123")
    }

    @Test
    fun testLetterDigits(){
        checkSimilarTo("abc123")
    }


    @Test
    fun testUpperCaseString(){
        checkSimilarTo("ABCD")
    }


    @Test
    fun testQuantifierSingle(){
        checkSimilarTo("a{2}")
    }


    @Test
    fun testQuantifierRange(){
        checkSimilarTo("a{3,5}")
    }

    @Test
    fun testQuantifierOnlyMin(){
        checkSimilarTo("a{2,}")
    }

    @Test
    fun testQuantifierStar(){
        checkSimilarTo("a*")
    }

    @Test
    fun testQuantifierPlus(){
        checkSimilarTo("a+")
    }

    @Test
    fun testQuantifierOptional(){
        checkSimilarTo("a?")
    }

    @Test
    fun testQuantifierCombined(){
        checkSimilarTo("a*b+c{1}d{2,}e{2,100}")
    }

    @Test
    fun testParentheses(){
        checkSimilarTo("()")
    }

    @Test
    fun testParenthesesWithText(){
        checkSimilarTo("(hello)")
    }

    @Test
    fun testParenthesesSequence(){
        checkSimilarTo("(a)(b)(c)")
    }

    @Test
    fun testParenthesesNested(){
        checkSimilarTo("(a(bc)(d))")
    }

    @Test
    fun testParenthesesWithQuantifiers(){
        checkSimilarTo("(a1)*(bc)+(d2)?")
    }

    @Test
    fun testDisjunction(){
        checkSimilarTo("a|b")
    }

    @Test
    fun testDisjunctionSequence(){
        checkSimilarTo("a|b|c|def|gh")
    }

    @Test
    fun testDisjunctionNested(){
        checkSimilarTo("(a(b|c))d")
    }

    @Test
    fun testClassRangeSingleChar(){
        checkSimilarTo("[a]")
    }

    @Test
    fun testClassRangeMultiChars(){
        checkSimilarTo("[abc]")
    }

    @Test
    fun testClassRangeMultiCharsWithSpecialSymbols(){
        checkSimilarTo("[abc123(){}/?+*]")
    }

    @Test
    fun testClassRangeChars(){
        checkSimilarTo("[a-z]")
    }

    @Test
    fun testClassRangeDigits(){
        checkSimilarTo("[0-9]")
    }

    @Test
    fun testClassRangeMulti(){
        checkSimilarTo("[a-zA-Z0-9]")
    }

    @Test
    fun testClassRangeQuantifier(){
        checkSimilarTo("[0-9]{2}")
    }


    //------------ different behavior from Java Regex  ----------------------

    @Test
    fun testAnyChar(){
        checkSimilarTo("_",".")
    }

    @Test
    fun testAnyCharMulti(){
        checkSimilarTo("___","...")
    }

    @Test
    fun testAnyCharMixed(){
        checkSimilarTo("_a_b_c_",".a.b.c.")
    }

    @Test
    fun testPercent(){
        checkSimilarTo("%",".*")
    }

    @Test
    fun testPercentMixed(){
        checkSimilarTo("%a%b%",".*a.*b.*")
    }

    @Test
    fun testClassRangeIndExample(){
        checkSimilarTo("/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?",
                "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?")
    }
}