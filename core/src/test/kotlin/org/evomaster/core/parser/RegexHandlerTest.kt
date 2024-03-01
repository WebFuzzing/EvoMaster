package org.evomaster.core.parser

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.client.java.instrumentation.heuristic.ValidatorHeuristics
import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.regex.Pattern

internal class RegexHandlerTest{


    @Test
    fun testCwaIssue(){
        //should not throw exception
        val s = "^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{10}$"
        RegexHandler.createGeneForJVM(s)
        RegexHandler.createGeneForJVM(RegexSharedUtils.handlePartialMatch(s))

        val x = "^[XxA-Fa-f0-9]([A-Fa-f0-9]{63})$"
        RegexHandler.createGeneForJVM(x)
        RegexHandler.createGeneForJVM(RegexSharedUtils.forceFullMatch(x))

        RegexHandler.createGeneForJVM(RegexSharedUtils.handlePartialMatch(s)+"|"+RegexSharedUtils.forceFullMatch(x))
    }

    @Test
    fun testInd1Issue(){
        val s = "^1[3-9]\\d{9}"
        val regex = RegexHandler.createGeneForJVM(s)
        assertEquals("${RegexGene.JAVA_REGEX_PREFIX}$s", regex.sourceRegex)
        assertThrows<ParseCancellationException>{RegexHandler.createGeneForJVM(RegexSharedUtils.handlePartialMatch(s))}
        RegexHandler.createGeneForJVM(RegexSharedUtils.forceFullMatch(s))

        val rand = Randomness()
        regex.randomize(rand, false)
        assertTrue(regex.isLocallyValid())
        val copy = regex.copy()
        assertTrue(copy.isLocallyValid())
        assertEquals(regex.getValueAsRawString(), copy.getValueAsRawString())

        val apc = AdaptiveParameterControl()
        val mwc = MutationWeightControl()

        regex.doInitialize(rand)

        repeat(1000){
            regex.standardMutation(rand, apc, mwc)
            regex.copy().standardMutation(rand, apc, mwc)
        }
    }

    @Test
    fun testPostgresSimilarToEmailRegex() {
        val regex = "[A-Za-z0-9-.+]+@[A-Za-z0-9-.]+.[A-Za-z]{2,}"
        val pattern = Pattern.compile(regex)
        val regexGene = RegexHandler.createGeneForPostgresSimilarTo(regex)
        val rand = Randomness()

        repeat(1000){
            regexGene.randomize(rand,true)
            assertTrue(pattern.matcher(regexGene.getValueAsRawString()).find())
        }

    }

    @Test
    fun testRegexEmail() {
        val regex = ValidatorHeuristics.EMAIL_REGEX_PATTERN
        val pattern = Pattern.compile(regex)
        val gene = RegexHandler.createGeneForJVM(regex)
        val rand = Randomness()
        repeat (10000) {
            gene.randomize(rand,true)
            assertTrue(pattern.matcher(gene.getValueAsRawString()).find())
        }

    }


    @Test
    fun testRegexPlusSymbol() {
        val regex = "(\\+)+"
        val pattern = Pattern.compile(regex)
        val gene = RegexHandler.createGeneForJVM(regex)
        val rand = Randomness()
        repeat (1000) {
            gene.randomize(rand,true)
            assertTrue(pattern.matcher(gene.getValueAsRawString()).find())
        }
    }

    @Test
    fun testRegexDotSymbol() {
        val regex = "(\\.)+"
        val pattern = Pattern.compile(regex)
        val gene = RegexHandler.createGeneForJVM(regex)
        val rand = Randomness()
        repeat (1000) {
            gene.randomize(rand,true)
            assertTrue(pattern.matcher(gene.getValueAsRawString()).find())
        }
    }


    @Test
    fun testRegexCharacter() {
        val regex = "(a)+"
        val pattern = Pattern.compile(regex)
        val gene = RegexHandler.createGeneForJVM(regex)
        val rand = Randomness()
        repeat (1000) {
            gene.randomize(rand,true)
            assertTrue(pattern.matcher(gene.getValueAsRawString()).find())
        }

    }

}