package org.evomaster.core.parser

import org.evomaster.client.java.instrumentation.shared.RegexSharedUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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


}