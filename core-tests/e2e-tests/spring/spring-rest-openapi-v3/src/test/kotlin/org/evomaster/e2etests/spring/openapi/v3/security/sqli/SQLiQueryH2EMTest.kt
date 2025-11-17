package org.evomaster.e2etests.spring.openapi.v3.security.sqli


import com.foo.rest.examples.spring.openapi.v3.security.sqli.SQLiH2QueryController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SQLiQueryH2EMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
//            config.instrumentMR_SQL = false

            initClass(SQLiH2QueryController(), config)
        }
    }

    @Test
    fun testSQLiH2EM() {
        runTestHandlingFlakyAndCompilation(
            "SQLiH2EMTest",
            20
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            val faults = DetectedFaultUtils.getDetectedFaults(solution)

            Assertions.assertTrue(faults.size == 1)

            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            Assertions.assertTrue({ DefinedFaultCategory.SQL_INJECTION in faultCategories })

            Assertions.assertTrue(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/query/vulnerable"
            })

            Assertions.assertFalse(faults.any {
                it.category == DefinedFaultCategory.SQL_INJECTION
                        && it.operationId == "GET:/api/sqli/query/safe"
            })

        }
    }
}
