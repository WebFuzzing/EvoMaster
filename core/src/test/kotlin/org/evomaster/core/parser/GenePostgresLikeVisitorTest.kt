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
        check("_", ".")
    }

    @Test
    fun testAnyAndText(){
        check("_a_b_", ".a.b.")
    }

    @Test
    fun testPercent(){
        check("%", ".*")
    }

    @Test
    fun testPercentAndText(){
        check("%a%b%", ".*a.*b.*")
    }

    @Test
    fun testAnyAndPercent(){
        check("%a_b%", ".*a.b.*")
    }

    @Test
    fun testEscapedAny(){
        check("\\_", "_")
    }

    @Test
    fun testAnyWithAlsoEscape(){
        check("\\__", "_.")
    }

    @Test
    fun testEscapedPercent(){
        check("\\%", "%")
    }

    @Test
    fun testPercentWithAlsoEscape(){
        check("\\%%", "%.*")
    }
}