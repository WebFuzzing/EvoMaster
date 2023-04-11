package org.evomaster.core.parser

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RegexHandlerTest{


    @Test
    fun testCwaIssue(){
        //should not throw exception
        val s = "^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{10}$"
        RegexHandler.createGeneForJVM(s)
        RegexHandler.createGeneForJVM(RegexSharedUtils.handlePartialMatch(s));

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


}