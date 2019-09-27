package org.evomaster.core.parser


import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

/**
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresSimilarToVisitorTest : RegexTestTemplate(){

    override fun createGene(regex: String): RegexGene {
        return RegexHandler.createGeneForPostgresSimilarTo(regex)
    }

    @Test
    fun testEmpty(){
        checkSameAsJava("")
    }

    @Test
    fun testBaseStringSingleChar(){
        checkSameAsJava("a")
    }

    @Test
    fun testBaseStringMultiChar(){
        checkSameAsJava("abc")
    }

    @Test
    fun testSingleDigit(){
        checkSameAsJava("1")
    }

    @Test
    fun testMultiDigits(){
        checkSameAsJava("123")
    }

    @Test
    fun testLetterDigits(){
        checkSameAsJava("abc123")
    }


    @Test
    fun testUpperCaseString(){
        checkSameAsJava("ABCD")
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
        checkSameAsJava("a{2,}")
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
    fun testCanSamplePair(){
        checkCanSample("(a|b)(c|d)", listOf("ac", "ad", "bc", "bd"), 500)
    }

    //------------ different behavior from Java Regex  ----------------------

    @Test
    fun testAnyChar(){
        check("_",".")
    }

    @Test
    fun testAnyCharMulti(){
        check("___","...")
    }

    @Test
    fun testAnyCharMixed(){
        check("_a_b_c_",".a.b.c.")
    }

    @Test
    fun testPercent(){
        check("%",".*")
    }

    @Test
    fun testPercentMixed(){
        check("%a%b%",".*a.*b.*")
    }

    @Test
    fun testClassRangeIndExample(){
        check("/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?",
                "/foo/../bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?")
    }
}