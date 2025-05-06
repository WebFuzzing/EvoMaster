package org.evomaster.e2etests.spring.openapi.v3.examples.expectations

import com.foo.rest.examples.spring.openapi.v3.cluster.ClusterTestController
import io.restassured.RestAssured.given
import org.evomaster.core.EMConfig
import org.evomaster.core.Main
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ClusterEMTest : SpringTestBase() {
    //TODO: BMR - more expectations related tests are possible. This is a good place for them.

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ClusterTestController())
        }
    }

    @Test
    fun testRunManual(){
        given().accept("*/*")
                .get(baseUrlOfSut + "/api/cluster/path1/" + true)
                .then()
                .statusCode(200)

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/cluster/path2/" + false)
                .then()
                .statusCode(500)

    }

    @Test
    fun testRunEM(){

        val terminations = listOf("_faults", "_successes")

        runTestHandlingFlakyAndCompilation(
                "ClusterEM",
                "org.foo.ClusterEM",
                terminations,
                100
        ){args: MutableList<String> ->

            /*
            val injector = Main.init(args.toTypedArray())

            val config = injector.getInstance(EMConfig::class.java)
            config.testSuiteSplitType = EMConfig.TestSuiteSplitType.CLUSTER

            val solution = Main.run(injector) as Solution<RestIndividual>
*/

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)
        }
    }

    @Test
    fun testClusterManual(){

        // This is an initial test for the clustering bug. Once I find a way to replicate it.

        val terminations = listOf("_faults", "_successes")
        runTestHandlingFlakyAndCompilation(
                "ClusterEMTypeBug",
                "org.foo.ClusterEMTypeBug",
                terminations,
                100
        ){args: MutableList<String> ->
            val injector = Main.init(args.toTypedArray())

            val config = injector.getInstance(EMConfig::class.java)
            config.testSuiteSplitType = EMConfig.TestSuiteSplitType.FAULTS

            val controllerInfoDto = Main.checkState(injector)

            val solution = Main.run(injector, controllerInfoDto) as Solution<RestIndividual>

            Main.writeTests(injector, solution, controllerInfoDto)

            //val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)
        }
    }
}