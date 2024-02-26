package org.evomaster.e2etests.spring.graphql.db.exisitingdata

import com.foo.graphql.db.exisitingdata.ExistingDataController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class ExistingDataEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExistingDataController())
        }
    }

    @Test
    fun testEM(){
        runTestHandlingFlakyAndCompilation(
            "GQL_DbExistingDataEM",
            "org.foo.graphql.DbExistingDataEM",
            500
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--heuristicsForSQL")
            args.add("true")
            args.add("--generateSqlDataWithSearch")
            args.add("true")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, "getY", GQMethodType.QUERY, 200, listOf(
                "\"x\":{\"id\":\"42\",\"name\":\"Foo\"}",
                "\"x\":{\"name\":\"Foo\"}",
                "\"x\":{\"id\":\"42\"}"
            ), false)
            assertHasAtLeastOne(solution, "getY", GQMethodType.QUERY, 200, "\"getY\":[]")
            assertNoneWithErrors(solution)
        }

    }
}