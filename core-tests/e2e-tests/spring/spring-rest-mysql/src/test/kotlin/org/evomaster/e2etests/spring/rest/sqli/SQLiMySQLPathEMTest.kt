package org.evomaster.e2etests.spring.rest.sqli

import com.foo.spring.rest.mysql.sqli.SQLiMySQLPathController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.mysql.entity.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SQLiMySQLPathEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SQLiMySQLPathController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "SQLiMySQLPathEM",
            100
        ) { args ->
            setOption(args, "security", "true")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

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
