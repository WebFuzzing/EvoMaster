package org.evomaster.core.parser.visitor

import org.evomaster.core.search.gene.regex.CharacterClassEscapeRxGene
import org.evomaster.core.search.gene.regex.PatternCharacterBlock
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Ecma262HandlerTest{

    private fun checkRegex(regex: String) : RegexGene{

        val randomness = Randomness()
        randomness.updateSeed(42)

        val gene = Ecma262Handler.createGene(regex)
        gene.randomize(randomness, false, listOf())

        val instance = gene.getValueAsRawString()

        /*
            Ecma262 and Java regex are not exactly the same.
            But for the base types we test in this class, they
            should be equivalent.
         */
        assertTrue(instance.matches(Regex(regex)),
                "String not matching regex:\n$regex\n$instance")

        return gene
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


}