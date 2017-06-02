package org.evomaster.core

import org.evomaster.clientJava.controllerApi.ControllerConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EMConfigTest{

    private val controllerPortOption = "sutControllerPort"

    @Test
    fun testGetOptionParserBase(){

        val options = EMConfig.getOptionParser()

        assertTrue(options.recognizedOptions().containsKey(controllerPortOption))
    }


    @Test
    fun testGetWithDefault(){

        val parser = EMConfig.getOptionParser()

        val portOpt = parser.recognizedOptions()[controllerPortOption] ?:
                throw Exception("Cannot find option")

        val options = parser.parse()

        assertEquals(""+ControllerConstants.DEFAULT_CONTROLLER_PORT, portOpt.value(options))
    }

    @Test
    fun testChangeIntProperty(){

        val parser = EMConfig.getOptionParser()

        val opt = parser.recognizedOptions()[controllerPortOption] ?:
                throw Exception("Cannot find option")

        val x = "42"
        val options = parser.parse("--"+controllerPortOption, x)

        assertEquals(x, opt.value(options))

        val config = EMConfig()
        assertTrue("" + config.sutControllerPort != x)

        config.updateProperties(options)
        assertEquals("" + config.sutControllerPort, x)
    }


    @Test
    fun testChangeLongProperty(){

        val parser = EMConfig.getOptionParser()

        val opt = parser.recognizedOptions()["seed"] ?:
                throw Exception("Cannot find option")

        val x = "42"
        val options = parser.parse("--seed", x)

        assertEquals(x, opt.value(options))

        val config = EMConfig()
        assertTrue("" + config.seed != x)

        config.updateProperties(options)
        assertEquals("" + config.seed, x)
    }

    @Test
    fun testChangeEnum(){

        val parser = EMConfig.getOptionParser()

        val opt = parser.recognizedOptions()["algorithm"] ?:
                throw Exception("Cannot find option")

        val config = EMConfig()

        var x = "MIO"
        var options = parser.parse("--algorithm", x)
        assertEquals(x, opt.value(options))

        config.updateProperties(options)
        assertEquals("" + config.algorithm, x)

        x = "RANDOM"
        options = parser.parse("--algorithm", x)
        assertEquals(x, opt.value(options))
        config.updateProperties(options)
        assertEquals("" + config.algorithm, x)
    }
}