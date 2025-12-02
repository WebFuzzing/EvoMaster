package org.evomaster.e2etests.spring.openapi.v3.security.sqli


import com.foo.rest.examples.spring.openapi.v3.security.sqli.SQLiH2PathController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SQLiPathH2EMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SQLiH2PathController())
        }
    }

    @Test
    fun testSQLiH2EM() {
        runTestHandlingFlakyAndCompilation(
            "SQLiPathH2EMTest",
            20
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "sqli", "true")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            Assertions.assertTrue(faults.size == 1)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertTrue({ DefinedFaultCategory.SQL_INJECTION in faultCategories })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/path/vulnerable/{id}"
            })

            assertFalse(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/path/safe"
            })

        }
    }
}
