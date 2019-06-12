package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

/**
 * Created by arcuri82 on 12-Jun-19.
 */
class GenePostgresLikeVisitorTest{


    private fun checkLike(like: String, javaRegex: String) : RegexGene {

        val randomness = Randomness()

        val gene = RegexHandler.createGeneForPostgresLike(like)

        for(seed in 1..100L) {
            randomness.updateSeed(seed)

            gene.randomize(randomness, false, listOf())

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            assertTrue(matcher.find(), "String not matching LIKE:\n$like\n$instance")
        }

        return gene
    }

    @Test
    fun testEmpty(){
        checkLike("","")
    }

    @Test
    fun testBaseStringSingleChar(){
        checkLike("a", "a")
    }

    @Test
    fun testBaseStringMultiChar(){
        checkLike("abc", "abc")
    }

    @Test
    fun testSingleDigit(){
        checkLike("1", "1")
    }

    @Test
    fun testAny(){
        checkLike("_", ".")
    }

    @Test
    fun testAnyAndText(){
        checkLike("_a_b_", ".a.b.")
    }

    @Test
    fun testPercent(){
        checkLike("%", ".*")
    }

    @Test
    fun testPercentAndText(){
        checkLike("%a%b%", ".*a.*b.*")
    }

    @Test
    fun testAnyAndPercent(){
        checkLike("%a_b%", ".*a.b.*")
    }

    @Test
    fun testEscapedAny(){
        checkLike("\\_", "_")
    }

    @Test
    fun testAnyWithAlsoEscape(){
        checkLike("\\__", "_.")
    }

    @Test
    fun testEscapedPercent(){
        checkLike("\\%", "%")
    }

    @Test
    fun testPercentWithAlsoEscape(){
        checkLike("\\%%", "%.*")
    }
}