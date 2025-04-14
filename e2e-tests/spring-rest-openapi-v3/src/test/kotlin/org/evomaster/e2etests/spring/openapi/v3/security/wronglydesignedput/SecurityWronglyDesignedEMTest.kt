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


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "SecurityWronglyDesignedEM",
                1000
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--security")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--configPath")
            args.add("src/main/resources/config/securitylogin.yaml")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
        }
    }
}