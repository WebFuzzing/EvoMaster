package org.evomaster.core

import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.lang.IllegalArgumentException

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

        assertEquals(""+ ControllerConstants.DEFAULT_CONTROLLER_PORT, portOpt.value(options))
    }

    @Test
    fun testChangeIntProperty(){

        val parser = EMConfig.getOptionParser()

        val opt = parser.recognizedOptions()[controllerPortOption] ?:
                throw Exception("Cannot find option")

        val x = "42"
        val options = parser.parse("--$controllerPortOption", x)

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

    @Test
    fun testEmptyFields(){

        val parser = EMConfig.getOptionParser()
        val opt = parser.recognizedOptions()["bbTargetUrl"] ?:
            throw Exception("Cannot find option")


        val config = EMConfig()
        assertEquals("", config.bbTargetUrl)

        val options = parser.parse()
        assertEquals("", opt.value(options))
    }

    @ParameterizedTest
    @ValueSource(strings = ["probOfRandomSampling", "startPerOfCandidateGenesToMutate", "focusedSearchActivationTime"])
    fun testProbability(name: String){

        val parser = EMConfig.getOptionParser()
        val opt = parser.recognizedOptions()[name] ?:
            throw Exception("Cannot find option")

        val config = EMConfig()
        val k = config.probOfRandomSampling
        assertTrue(k>=0 && k<=1)

        val p = "0.3"
        var options = parser.parse("--$name", p)
        assertEquals(p, opt.value(options))

        config.updateProperties(options) // should be no problem, as valid p

        val wrong = "1.2"
        options = parser.parse("--$name", wrong)

        assertThrows(Exception::class.java, {config.updateProperties(options)}) // invalid p
    }


    @ParameterizedTest
    @ValueSource(strings = ["bbSwaggerUrl", "bbTargetUrl"])
    fun testUrl(name: String){

        val parser = EMConfig.getOptionParser()
        val opt = parser.recognizedOptions()[name] ?:
            throw Exception("Cannot find option")
        val config = EMConfig()

        val ok = "http://localhost:8080"
        var options = parser.parse("--$name", ok)
        assertEquals(ok, opt.value(options))
        config.updateProperties(options) // no exception

        val noPort = "http://localhost"
        options = parser.parse("--$name", noPort)
        assertEquals(noPort, opt.value(options))
        config.updateProperties(options) // no exception

        val wrong = "foobar"
        options = parser.parse("--$name", wrong)
        assertThrows(Exception::class.java, {config.updateProperties(options)})

        val noProtocol = "localhost:8080"
        options = parser.parse("--$name", noProtocol)
        assertThrows(Exception::class.java, {config.updateProperties(options)})
    }


    @ParameterizedTest
    @ValueSource(strings = [""," ","1","42","-42s","1 42s","42s1m","1m 42s"])
    fun testTimeRegexWrong(value: String){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["maxTime"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--maxTime", value)
        assertThrows(Exception::class.java, {config.updateProperties(options)})
    }

    @Test
    fun testTimeRegexJustSeconds(){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["maxTime"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--maxTime", "42s")
        config.updateProperties(options)

        val seconds = config.timeLimitInSeconds()
        assertEquals(42, seconds)
    }

    @Test
    fun testTimeRegexJustMinutes(){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["maxTime"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--maxTime", "3m")
        config.updateProperties(options)

        val seconds = config.timeLimitInSeconds()
        assertEquals(180, seconds)
    }

    @Test
    fun testTimeRegexJustHours(){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["maxTime"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--maxTime", "2h")
        config.updateProperties(options)

        val seconds = config.timeLimitInSeconds()
        assertEquals(2 * 60 * 60, seconds)
    }

    @Test
    fun testTimeRegex(){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["maxTime"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--maxTime", " 1h10m120s  ")
        config.updateProperties(options)

        val seconds = config.timeLimitInSeconds()
        assertEquals( (60 * 60) + 600 + 120, seconds)
    }


    @ParameterizedTest
    @ValueSource(strings =  ["","1",".a","a.","a..a","a.1","?","!","%"])
    fun testFileNameWrong(value : String){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["testSuiteFileName"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--testSuiteFileName", value)

        assertThrows(Exception::class.java) {config.updateProperties(options)}
    }

    @ParameterizedTest
    @ValueSource(strings =  ["a","a.a","a.a1","A","a123","a.b.c.d1.e123"])
    fun testFileNameOK(value : String){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["testSuiteFileName"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--testSuiteFileName", value)

        config.updateProperties(options)

        assertEquals(value, config.testSuiteFileName)
    }


    @Test
    fun testMinusInFileName(){

        val name = "a-a"

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["testSuiteFileName"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--testSuiteFileName", name)

        config.outputFormat = OutputFormat.JAVA_JUNIT_4
        assertThrows(Exception::class.java) {config.updateProperties(options)}

        //TODO update when ll support JavaScript
//        config.outputFormat = TODO JS
//        config.updateProperties(options)
//        assertEquals(name, config.testSuiteFileName)
    }


    @ParameterizedTest
    @ValueSource(strings =  ["pom.xml"])
    fun testWrongPath(value : String){

        /*
            Interestingly, a name like
            ?*!$%@
            would fail in Windows, but it is OK in Mac...
            Mac practically accepts anything:
            https://superuser.com/questions/326103/what-are-invalid-characters-for-a-file-name-under-os-x
         */

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["outputFolder"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--outputFolder", value)

        assertThrows(Exception::class.java) {config.updateProperties(options)}
    }
}