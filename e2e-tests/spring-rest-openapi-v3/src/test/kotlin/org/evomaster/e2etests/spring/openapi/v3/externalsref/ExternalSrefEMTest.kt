package org.evomaster.e2etests.spring.openapi.v3.externalsref

import com.foo.rest.examples.spring.openapi.v3.externalsref.ExternalSrefController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 */
class ExternalSrefEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExternalSrefController())
        }
    }


    @Test
    fun testRunEM() {

        // problem with hard-coded port in schema $ref.
        // so no compilation, and no flaky-handling
        val args  = getArgsWithCompilation(20,"ExternalSrefEM", ClassName("org.foo.ExternalSrefEMTest"),false)

        val solution = initAndRun(args)

        assertTrue(solution.individuals.size >= 1)
        assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/externalsref", "OK")

        //make sure schema validation works when dealing wiht external $ref
        val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
        assertEquals(1, faults.size)
        assertEquals(FaultCategory.SCHEMA_INVALID_RESPONSE, faults.first())
    }
}