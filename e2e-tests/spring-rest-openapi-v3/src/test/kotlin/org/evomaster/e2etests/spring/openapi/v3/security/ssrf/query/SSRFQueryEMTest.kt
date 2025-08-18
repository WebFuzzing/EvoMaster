package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.query

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.query.SSRFQueryController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SSRFQueryEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SSRFQueryController())
        }
    }

    @Disabled
    @Test
    fun testSSRFQuery() {

    }
}
