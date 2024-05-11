package org.evomaster.core

import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.InetAddress
import java.net.URL

internal class EMConfigTest{

    private val controllerPortOption = "sutControllerPort"
    private val endpointFocus = "endpointFocus"
    private val endpointPrefix = "endpointPrefix"

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
    @ValueSource(strings = ["probOfRandomSampling", "focusedSearchActivationTime", "startingPerOfGenesToMutate", "d"])
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


    @Test
    fun testUrl(){

        val name = "bbSwaggerUrl"

        val parser = EMConfig.getOptionParser()
        val opt = parser.recognizedOptions()[name] ?:
            throw Exception("Cannot find option")
        val config = EMConfig()

        val ok = "http://localhost:8080"
        var options = parser.parse("--$name", ok, "--blackBox","true","--outputFormat","JAVA_JUNIT_4")
        assertEquals(ok, opt.value(options))
        config.updateProperties(options) // no exception

        val noPort = "http://localhost"
        options = parser.parse("--$name", noPort, "--blackBox","true","--outputFormat","JAVA_JUNIT_4")
        assertEquals(noPort, opt.value(options))
        config.updateProperties(options) // no exception

        val wrong = "foobar"
        options = parser.parse("--$name", wrong, "--blackBox","true","--outputFormat","JAVA_JUNIT_4")
        assertThrows(Exception::class.java, {config.updateProperties(options)})

        val noProtocol = "localhost:8080"
        options = parser.parse("--$name", noProtocol, "--blackBox","true","--outputFormat","JAVA_JUNIT_4")
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
        parser.recognizedOptions()["outputFilePrefix"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--outputFilePrefix", value)

        assertThrows(Exception::class.java) {config.updateProperties(options)}
    }

    @ParameterizedTest
    @ValueSource(strings =  ["a","a.a","a.a1","A","a123","a.b.c.d1.e123"])
    fun testFileNameOK(value : String){

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["outputFilePrefix"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--outputFilePrefix", value)

        config.updateProperties(options)

        assertEquals(value, config.outputFilePrefix)
    }


    @Test
    fun testMinusInFileName(){

        val name = "a-a"

        val parser = EMConfig.getOptionParser()
        parser.recognizedOptions()["outputFilePrefix"] ?: throw Exception("Cannot find option")

        val config = EMConfig()
        val options = parser.parse("--outputFilePrefix", name)

        config.outputFormat = OutputFormat.JAVA_JUNIT_4
        assertThrows(Exception::class.java) {config.updateProperties(options)}

        //in JavaScript, it is allowed (and common?) to have '-' in file names
        config.outputFormat = OutputFormat.JS_JEST
        config.updateProperties(options)
        assertEquals(name, config.outputFilePrefix)

        //TODO check C#
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

    @Test
    fun testValidTargetExclusions(){
        val parser = EMConfig.getOptionParser()

        parser.recognizedOptions()["excludeTargetsForImpactCollection"] ?: throw Exception("Cannot find option")

        val config = EMConfig()

        var options = parser.parse("--excludeTargetsForImpactCollection", "none")
        config.updateProperties(options)
        assertTrue(config.excludedTargetsForImpactCollection.isEmpty())

        (1..2).forEach { n->
            IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.indices.forEach { index ->
                if (index+n <= IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.size){
                    val candidates = IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.subList(index, index+n)
                    options = parser.parse("--excludeTargetsForImpactCollection", candidates.joinToString(";"))
                    config.updateProperties(options)
                    assertEquals(n, config.excludedTargetsForImpactCollection.size)
                    assertTrue(config.excludedTargetsForImpactCollection.containsAll(candidates))

                    options = parser.parse("--excludeTargetsForImpactCollection",
                        candidates.joinToString(";") { it.lowercase() })
                    config.updateProperties(options)
                    assertEquals(n, config.excludedTargetsForImpactCollection.size)
                    assertTrue(config.excludedTargetsForImpactCollection.containsAll(candidates))

                    options = parser.parse("--excludeTargetsForImpactCollection", candidates.joinToString(";"){it.uppercase()})
                    config.updateProperties(options)
                    assertEquals(n, config.excludedTargetsForImpactCollection.size)
                    assertTrue(config.excludedTargetsForImpactCollection.containsAll(candidates))
                }
            }
        }
    }


    @Test
    fun testInvalidTargetExclusions(){
        val parser = EMConfig.getOptionParser()

        val config = EMConfig()

        var options = parser.parse("--excludeTargetsForImpactCollection", ",,;,,")
        assertThrows(Exception::class.java) {config.updateProperties(options)}

        (1..2).forEach { n->
            IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.indices.forEach { index ->

                if (index+n <= IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.size){

                    val candidates = IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.subList(index, index+n)

                    // none is not allowed to combine with others
                    options = parser.parse("--excludeTargetsForImpactCollection", listOf("None").plus(candidates).joinToString(";"))
                    assertThrows(Exception::class.java) {config.updateProperties(options)}

                    if (n > 1){
                        // invalid separator
                        options = parser.parse("--excludeTargetsForImpactCollection", candidates.joinToString(","))
                        assertThrows(Exception::class.java) {config.updateProperties(options)}
                    }

                    // target prefix does not exist
                    options = parser.parse("--excludeTargetsForImpactCollection", candidates.joinToString(";"){"${it}foo"})
                    assertThrows(Exception::class.java) {config.updateProperties(options)}

                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["number.toml","number.yml"])
    fun testParamsInConfigFile(fileName: String){
        val parser = EMConfig.getOptionParser()
        val config = EMConfig()

        config.populationSize = 77
        val options = parser.parse("--configPath", "src/test/resources/config/$fileName")
        config.updateProperties(options)
        assertEquals(42, config.populationSize)
    }

    @ParameterizedTest
    @ValueSource(strings = ["number.toml","number.yml"])
    fun testCLIOverrideParamsInConfigFile(fileName: String){
        val parser = EMConfig.getOptionParser()
        val config = EMConfig()

        val n = 1234
        config.populationSize = 77
        val options = parser.parse("--configPath", "src/test/resources/config/$fileName", "--populationSize","$n")
        config.updateProperties(options)
        assertEquals(n, config.populationSize)
    }

    @Test
    fun testWrongParamInConfigFile(){
        val parser = EMConfig.getOptionParser()
        val config = EMConfig()

        val options = parser.parse("--configPath", "src/test/resources/config/foo.toml")
        val t = assertThrows(ConfigProblemException::class.java) {config.updateProperties(options)}
        assertTrue(t.message!!.contains("non-existing properties"), t.message)
    }

    @Test
            /*
            Check endpointFocus exists in EMConfig
            */
    fun testEndpointFocusExists() {

        val options = EMConfig.getOptionParser()
        assertTrue(options.recognizedOptions().containsKey(endpointFocus))
    }

    @Test
            /*
            Check endpointPrefix exists in EMConfig
             */
    fun testEndpointPrefixExists() {

        val options = EMConfig.getOptionParser()
        assertTrue(options.recognizedOptions().containsKey(endpointPrefix))
    }

    @Test
            /*
            The user provides neither endpointFocus nor EndpointPrefix.
            In this case both endpointFocus and endpointPrefix should be NULL
            */
    fun testNoEndpointFocusNoEndpointPrefix() {

        val parser = EMConfig.getOptionParser()
        val options = parser.parse()
        val endpointFocusVal = options.valueOf(endpointFocus)
        val endpointPrefixVal = options.valueOf(endpointPrefix)

        // both endpointFocus and endpointPrefix are null
        assertEquals("null", endpointFocusVal)
        assertEquals("null", endpointPrefixVal)
    }

    /*
    The user does not provide endpointFocus, but provides endpointPrefix
     */
    @Test
    fun testNoEndpointFocusEndpointPrefix() {

        val sampleEndpointPrefix = "/endPointPrefixSample"
        val parser = EMConfig.getOptionParser()
        val options = parser.parse("--$endpointPrefix", sampleEndpointPrefix)
        val endpointFocusVal = options.valueOf(endpointFocus)
        val endpointPrefixVal = options.valueOf(endpointPrefix)

        // both endpointFocus and endpointPrefix are null
        assertEquals("null", endpointFocusVal)
        assertEquals(sampleEndpointPrefix, endpointPrefixVal)
    }

    @Test
            /*
            The user provides endpointFocus, but does not provide endpointPrefix
             */
    fun testEndpointFocusNoEndpointPrefix() {

        val parser = EMConfig.getOptionParser()
        val sampleEndpointFocus = "/endpointFocusSample"
        val options = parser.parse("--$endpointFocus", sampleEndpointFocus)
        val endpointFocusVal = options.valueOf(endpointFocus)
        val endpointPrefixVal = options.valueOf(endpointPrefix)

        // both endpointFocus and endpointPrefix are null
        assertEquals(sampleEndpointFocus, endpointFocusVal)
        assertEquals("null", endpointPrefixVal)
    }

    @Test
            /*
            The user provides both endpointFocus and endpointPrefix. In that case, an exception
            should be thrown.
             */
    fun testEndPointToFocusEndpointToPrefix() {

        val sampleEndpointFocus = "/endpointFocusSample"
        val sampleEndpointPrefix = "/endPointPrefixSample"

        assertThrows(
            ConfigProblemException::class.java
        ) {
            val params = arrayOf("--$endpointFocus", sampleEndpointFocus,
                "--$endpointPrefix", sampleEndpointPrefix)
            EMConfig.validateOptions(params)
        }
    }

    @Test
    fun testInvalidExternalServiceIP() {
        val params = arrayOf("--externalServiceIP", "128.0.0.0")
        assertThrows(ConfigProblemException::class.java) {
            EMConfig.validateOptions(params)
        }
    }

    @Test
    fun testHighestExternalServiceIP() {
        val params = arrayOf("--externalServiceIP", "127.255.255.255")
        EMConfig.validateOptions(params)
    }

    @Test
    fun testLowestExternalServiceIP() {
        val params = arrayOf("--externalServiceIP", "127.0.0.4")
        EMConfig.validateOptions(params)
    }

    @ParameterizedTest
    @ValueSource(strings = ["127.0.000.5","000127.0.00.5"])
    fun testLeadingZeros(ipAddress: String){
        //leading zeros are accepted
        //https://superuser.com/questions/857603/are-ip-addresses-with-and-without-leading-zeroes-the-same
        //https://superuser.com/questions/929153/leading-zeros-in-ipv4-address-is-that-a-no-no-by-convention-or-standard
        InetAddress.getByName(ipAddress) // should throw no exception
        val params = arrayOf("--externalServiceIP", ipAddress)
        EMConfig.validateOptions(params)
    }

    @ParameterizedTest
    @ValueSource(strings = ["127.0.0.1","127.0.0.2","0127.0.00.1","127.0.0.002","127.0.0.0","127.0.0.3"])
    fun testTooLowValues(ipAddress: String){
        InetAddress.getByName(ipAddress) // should throw no exception

        val params = arrayOf("--externalServiceIP", ipAddress)
        assertThrows(ConfigProblemException::class.java) {
            EMConfig.validateOptions(params)
        }
    }



    @ParameterizedTest
    @ValueSource(strings = [ "127.0.2.161","127.0.2.1","127.0.1.181","127.0.1.161","127.0.1.1","127.0.0.182","127.0.0.162","127.0.6.181","127.0.6.161","127.0.6.1","127.0.5.181","127.0.5.161","127.0.5.1","127.0.4.181","127.0.9.1","127.0.8.181","127.0.8.161","127.0.8.1","127.0.7.181","127.0.7.161","127.0.7.1","127.0.11.1","127.0.10.181","127.0.10.161","127.0.10.1","127.0.9.181","127.0.9.161","127.0.4.161","127.0.4.1","127.0.3.181","127.0.3.161","127.0.3.1","127.0.2.181","127.0.2.161"])
    fun testValidExternalServiceIPs(ipAddress: String) {
        InetAddress.getByName(ipAddress) // should throw no exception
        val params = arrayOf("--externalServiceIP", ipAddress)
        EMConfig.validateOptions(params)
    }




}
