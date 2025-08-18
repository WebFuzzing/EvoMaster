package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.header

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.header.SSRFHeaderController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SSRFHeaderEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SSRFHeaderController())
        }
    }

    @Disabled
    @Test
    fun testSSRFHeader() {

    }
}
