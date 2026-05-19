package org.evomaster.e2etests.spring.openapi.v3.httporacle.nonidempotentput

import com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.json.HttpNonIdempotentPutController
import com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.urlencoded.HttpNonIdempotentPutUrlencodedController
import com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.xml.HttpNonIdempotentPutXMLController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpNonIdempotentPutUrlencodedEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpNonIdempotentPutUrlencodedController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpNonIdempotentPutUrlencodedEM",
                1000
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(1, faults.size)
            assertEquals(ExperimentalFaultCategory.HTTP_NON_IDEMPOTENT_PUT, faults.first())
        }
    }
}