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
        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

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
        val options = parser.parse("--endpointPrefix", sampleEndpointPrefix)
        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

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
        val options = parser.parse("--endpointFocus", sampleEndpointFocus)
        val endpointFocusVal = options.valueOf("endpointFocus")
        val endpointPrefixVal = options.valueOf("endpointPrefix")

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
            IllegalArgumentException::class.java
        ) {
            val params = arrayOf("--endpointFocus", sampleEndpointFocus,
                "--endpointPrefix", sampleEndpointPrefix)
            EMConfig.validateOptions(params)
        }
    }
}
