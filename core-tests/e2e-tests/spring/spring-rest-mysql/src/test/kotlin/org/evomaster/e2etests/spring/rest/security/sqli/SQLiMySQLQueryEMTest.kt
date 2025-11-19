package org.evomaster.e2etests.spring.rest.security.sqli

import com.foo.spring.rest.mysql.security.sqli.SQLiMySQLQueryController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.mysql.entity.SpringTestBase
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SQLiMySQLQueryEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            config.instrumentMR_SQL = false

            initClass(SQLiMySQLQueryController(), config)
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "SQLiMySQLQueryEM",
            100
        ) { args ->
            setOption(args, "security", "true")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            assertTrue(faults.size == 1)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertTrue({ DefinedFaultCategory.SQL_INJECTION in faultCategories })

            assertTrue(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/query/vulnerable"
            })

            assertFalse(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/query/safe"
            })

        }
    }
}
