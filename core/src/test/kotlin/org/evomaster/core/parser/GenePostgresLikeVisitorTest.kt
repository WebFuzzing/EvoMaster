package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.RegexGene
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresLikeVisitorTest : RegexTestTemplate(){

    override fun createGene(regex: String) : RegexGene {
        return RegexHandler.createGeneForPostgresLike(regex)
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
    fun testAny(){
        checkMatches("_", ".")
    }

    @Test
    fun testAnyAndText(){
        checkMatches("_a_b_", ".a.b.")
    }

    @Test
    fun testPercent(){
        checkMatches("%", ".*")
    }

    @Test
    fun testPercentAndText(){
        checkMatches("%a%b%", ".*a.*b.*")
    }

    @Test
    fun testAnyAndPercent(){
        checkMatches("%a_b%", ".*a.b.*")
    }

    @Test
    fun testEscapedAny(){
        checkMatches("\\_", "_")
        checkSamplesExactly("\\_", "_")
        checkSamplesExactly("a\\_b", "a_b")
    }

    @Test
    fun testAnyWithAlsoEscape(){
        checkMatches("\\__", "_.")
    }

    @Test
    fun testEscapedPercent(){
        checkMatches("\\%", "%")
        checkSamplesExactly("\\%", "%")
        checkSamplesExactly("100\\%", "100%")
    }

    @Test
    fun testPercentWithAlsoEscape(){
        checkMatches("\\%%", "%.*")
    }

    @Test
    fun testBlankBlockName(){
        checkSameAsJava(" ")
        checkMatches("% \n%", ".* \n.*")
        checkMatches("% %", ".* .*")
    }
}