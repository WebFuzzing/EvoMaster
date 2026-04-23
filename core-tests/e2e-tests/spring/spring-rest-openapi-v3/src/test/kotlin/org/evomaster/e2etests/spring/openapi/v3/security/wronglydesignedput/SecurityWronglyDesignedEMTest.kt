package org.evomaster.e2etests.spring.openapi.v3.security.wronglydesignedput

import com.foo.rest.examples.spring.openapi.v3.security.wronglydesignedput.SecurityWronglyDesignedPutController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityWronglyDesignedEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SecurityWronglyDesignedPutController())
        }
    }

    /*
        This test is added to reproduce the IllegalStateException: Call expected a missing chained 'location'
        error during black box test generation. After creating a new individual for security checks, resource
        forward links may break, leading to this exception. Here, we just make sure EM doesn't crash.
    */
    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityWronglyDesignedEM",
                1000
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "configPath", "src/main/resources/config/securitylogin.yaml")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
        }
    }
}