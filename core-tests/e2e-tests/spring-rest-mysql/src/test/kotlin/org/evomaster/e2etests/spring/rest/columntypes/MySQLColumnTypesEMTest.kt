package org.evomaster.e2etests.spring.rest.columntypes

import com.foo.spring.rest.mysql.columntypes.MySQLColumnTypesController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MySQLColumnTypesEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initClass(MySQLColumnTypesController())
        }
    }

    @Test
    fun testRunEM() {

        val budget = 400
        runTestHandlingFlakyAndCompilation(
                "MySQLColumnTypesEM",
                "org.foo.spring.rest.mysql.columntypes.MySQLColumnTypesEM",
                budget
        ) { args ->

            args.add("--enableWeightBasedMutationRateSelectionForGene")
            args.add("false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/integerdatatypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/integerdatatypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/floatingpointtypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/floatingpointtypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/bitdatatype",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/bitdatatype",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/booleandatatypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/booleandatatypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/serialdatatype",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/serialdatatype",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/dateandtimetypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/dateandtimetypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/stringdatatypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/stringdatatypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/fixedpointtypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/fixedpointtypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/jsondatatypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/jsondatatypes",null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/mysql/spatialdatatypes",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200,"/api/mysql/spatialdatatypes",null)

        }
    }

}
