package org.evomaster.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class EMConfigTest{

    @Test
    fun testGetOptionParserBase(){

        val options = EMConfig.getOptionParser()

        assertTrue(options.recognizedOptions().containsKey("sutControllerPort"))
    }


}