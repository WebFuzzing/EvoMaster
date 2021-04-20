package org.evomaster.e2etests.spring.openapi.v3.examples.expectations

import com.foo.rest.examples.spring.openapi.v3.expectations.ExpectationsController
import io.restassured.RestAssured.given
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExpectationsEMTest : SpringTestBase() {
    //TODO: BMR - more expectations related tests are possible. This is a good place for them.

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExpectationsController())
        }
    }

    @Test
    fun testRunManual(){
        given().accept("*/*")
                .get(baseUrlOfSut + "/api/expectations/" + true)
                .then()
                .statusCode(200)

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/expectations/" + false)
                .then()
                .statusCode(500)

    }

    @Test
    fun testRunEM(){
        runTestHandlingFlakyAndCompilation(
                "ExpectationsEM",
                "org.foo.ExpectationsEM",
                100
        ){args: MutableList<String> ->
            args.add("--expectationsActive")
            args.add("" + true)
            args.add("--testSuiteSplitType")
            args.add("NONE")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)
        }
    }
}