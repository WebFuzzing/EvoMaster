package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.utils.RegexFlags
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.AssertionError
import java.lang.IllegalStateException
import java.util.regex.Pattern

/**
 * Created by arcuri82 on 11-Sep-19.
 */
abstract class RegexTestTemplate {

    protected abstract fun createGene(regex: String) : RegexGene

    protected fun checkSameAsJava(regex: String) : RegexGene {
        //used when syntax is the same as in Java regex
        return check(regex, regex)
    }

    protected fun checkSameAsJava(regex: String, externalRegexFlags: RegexFlags = RegexFlags()) : RegexGene {
        val randomness = Randomness().apply { updateSeed(42) }

        val gene = RegexHandler.createGeneForJVM(regex, externalRegexFlags)

        for(seed in 1..100L) {

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(regex, externalRegexFlags.toJavaFlagBitmask())
            val matcher = pattern.matcher(instance)
            Assertions.assertTrue(matcher.find(), "String not matching:\n$regex\n$instance")
        }

        return gene
    }

    protected fun check(regex: String, javaRegex: String) : RegexGene {
        val randomness = Randomness().apply { updateSeed(42) }

        val gene = createGene(regex)

        for(seed in 1..100L) {

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            val pattern = Pattern.compile(javaRegex)
            val matcher = pattern.matcher(instance)
            Assertions.assertTrue(matcher.find(), "String not matching:\n$regex\n$instance")
        }

        return gene
    }

    protected fun checkCanSample(regex: String, values: Collection<String>, tries: Int) {

        for(value in values){
            checkCanSample(regex, value, tries)
        }
    }

    protected fun checkCanSample(regex: String, value: String, tries: Int){

        val randomness = Randomness().apply { updateSeed(42) }

        val gene = createGene(regex)

        for(seed in 1L..tries) {

            gene.randomize(randomness, false)

            val instance = gene.getValueAsRawString()

            if(instance == value){
                return
            }
        }

        throw AssertionError("Cannot sample $value")
    }

    /**
     * For regex that can only match a fixed set of values, checks that every
     * sampled value is one of [expected], and that all of them do get sampled.
     */
    protected fun checkSamplesExactly(regex: String, vararg expected: String, tries: Int = 500) {
        val randomness = Randomness().apply { updateSeed(42) }
        val gene = createGene(regex)
        val sampled = mutableSetOf<String>()

        repeat(tries) {
            gene.randomize(randomness, false)
            sampled.add(gene.getValueAsRawString())
        }

        assertEquals(expected.toSet(), sampled, "Wrong values sampled for: $regex")
    }
}