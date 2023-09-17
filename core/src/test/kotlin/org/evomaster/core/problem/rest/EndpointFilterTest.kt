package org.evomaster.core.problem.rest

import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EndpointFilterTest {

    private val endpointFocus = "endpointFocus"
    private val endpointPrefix = "endpointPrefix"


    @Test
    /*
    Check endpointFocus exists in EMConfig
    */
    fun testEndpointFocusExists()
    {
        val options = EMConfig.getOptionParser()

        assertTrue(options.recognizedOptions().containsKey(endpointFocus))
    }


    @Test
    /*
    Check endpointPrefix exists in EMConfig
     */
    fun testEndpointPrefixExists()
    {
        val options = EMConfig.getOptionParser()

        assertTrue(options.recognizedOptions().containsKey(endpointPrefix))

    }

    @Test
    /*
    The user provides neither endpointFocusNorEndpointPrefix.
    In this case both endpointFocus and endpointPrefix should be NULL
    */
    fun testNoEndpointFocusNoEndpointPrefix()
    {

        val config = EMConfig()

        //config.validateOptions(params)

        val parser = EMConfig.getOptionParser()

        val options = parser.parse()

        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

        // both endpointFocus and endpointPrefix are null
        assertEquals("null", endpointFocusVal)
        assertEquals("null", endpointPrefixVal)
    }

    /*
    The user does not provide endpoint to focus, but
    endpoint to prefix
     */
    @Test
    fun testNoEndpointFocusEndpointPrefix()
    {
        //val parser = EMConfig.getOptionParser()

        val sampleEndpointPrefix = "/endPointPrefixSample"

        val parser = EMConfig.getOptionParser()

        val options = parser.parse("--endpointPrefix", sampleEndpointPrefix)

        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

        // both endpointFocus and endpointPrefix are null
        assertEquals("null", endpointFocusVal)
        assertEquals(sampleEndpointPrefix, endpointPrefixVal)
    }

    @Test
    /*
    The user provides endpoint to focus but does not
    provide endpoint to prefix
     */
    fun testEndpointFocusNoEndpointPrefix()
    {
        val parser = EMConfig.getOptionParser()

        val sampleEndpointFocus = "/endpointFocusSample"

        val options = parser.parse("--endpointFocus", sampleEndpointFocus)

        //EMConfig.check

        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

        // both endpointFocus and endpointPrefix are null
        assertEquals(sampleEndpointFocus, endpointFocusVal)
        assertEquals("null", endpointPrefixVal)
    }

    @Test
    /*
    The user provides both, but in that case, an exception
    should be thrown.
     */
    fun testEndPointToFocusEndpointToPrefix()
    {
        val parser = EMConfig.getOptionParser()

        val sampleEndpointFocus = "/endpointFocusSample"
        val sampleEndpointPrefix = "/endPointPrefixSample"

       // assertThrows (IllegalArgumentException, {
       //     val options = parser.parse(
       //         "--endpointFocus", sampleEndpointFocus,
       //         "--endpointPrefix", sampleEndpointPrefix
        //}

        assertThrows<IllegalArgumentException>(
            IllegalArgumentException::class.java
        ) {
            val params = arrayOf("--endpointFocus", sampleEndpointFocus,
                "--endpointPrefix", sampleEndpointPrefix)

            EMConfig.validateOptions(params)
        }


    }


}
