package org.evomaster.core.parser

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils
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
        assertEquals("regex $s", regex.name)
        assertThrows<ParseCancellationException>{RegexHandler.createGeneForJVM(RegexSharedUtils.handlePartialMatch(s))}
    }


}